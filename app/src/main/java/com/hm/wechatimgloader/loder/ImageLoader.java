package com.hm.wechatimgloader.loder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 图片加载类
 */

public class ImageLoader {


    /**
     *  该实例知识点
     *  1.Handler + MessageQueue
     *  2.线程池
     *  3.图片压缩
     *  4.信号量的好似用
     *  5.反射机制的使用
     */


    private static ImageLoader mInstance;


    private LruCache<String, Bitmap> mLruCache; //图片所用的内存说

    private ExecutorService mRunnablePool; //后台线程池
    private static final int MAX_DEFAULT_COUNT = 1;//最大线程数

    private Thread mLoopThread; //后台的轮询线程

    private Handler mPoolHandler; //异步线程Handler

    private Handler mMainHandler;//主线程Handler

    private Type mType = Type.LIFO;//线程执行顺序 FIFO 先进先出 FILO 先进后出

    private LinkedList<Runnable> mTaskQueue;//异步任务列表

    private Semaphore mPoolHandlerSemaphore = new Semaphore(0); //利用信号量控制 避免异步handler还没有初始化的时候就去调用它 避免空指针

    private Semaphore mPoolSemaphore ;
    public enum Type {
        FIFO, LIFO;

    }

    private ImageLoader(int threadCount, Type mType) //无参数构造方法
    {
        //变量初始化
        init(threadCount, mType);
    }

    /**
     * 变量初始化方法
     *
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {

        mLoopThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                //初始化异步线程的Handler
                mPoolHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //从线程池中取出一个任务来执行

                        mRunnablePool.execute(getTask());
                        try {
                            mPoolSemaphore.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                mPoolHandlerSemaphore.release();
                Looper.loop();
            }
        };
        mLoopThread.start(); //开启线程

        //初始化缓存参数
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMomory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMomory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        //初始化线程池
        mRunnablePool = Executors.newFixedThreadPool(threadCount);
        //初始化加载方式
        mType = type;
        //初始化异步任务队列
        mTaskQueue = new LinkedList<Runnable>();
        //初始化线程池信号量
        mPoolSemaphore = new Semaphore(threadCount);
    }

    /**
     * 从任务队列取出一个任务
     *
     * @return
     */
    private Runnable getTask() {
        if (mType == Type.FIFO) {

            return mTaskQueue.removeFirst(); //取第一个

        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();//去最后一个
        }
        return null;
    }

    /**
     * 单例方法
     *
     * @return
     */
    public static ImageLoader getInstance() {
        if (mInstance == null) {

            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(MAX_DEFAULT_COUNT, Type.LIFO);
                }
            }

        }
        return mInstance;

    }

    /**
     * 多态实现自定义最大线程池数量 和 加载策略
     * @return
     */
    public static ImageLoader getInstance(int threadCount, Type type) {
        if (mInstance == null) {

            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }

        }
        return mInstance;

    }
    /**
     * 根据url为ImageView设置图片
     *
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);
        if (mMainHandler == null) {
            mMainHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    // 获取得到的图片 拿到bitmap 回调设置图片
                    ImageBeanHolder beanHolder = (ImageBeanHolder) msg.obj;
                    Bitmap bmp = beanHolder.bitmap;
                    ImageView image = beanHolder.imageView;
                    String path = beanHolder.path;

                    if (image.getTag().toString().equals(path)) {
                        image.setImageBitmap(bmp);
                    }
                }
            };

        }
        Bitmap bm = getBitmapFromLruCacheByPath(path);
        if (bm != null) {
            refreshBitmap(bm, path, imageView);
        } else {
            addTasks(new Runnable() {
                @Override
                public void run() {
                    //获得图片需要显示的大小
                    ImageSize imageSize = getImageViewSize(imageView);
                    //压缩图片
                    Bitmap bm = decodeSampledBitmapFromPath(path, imageSize.width, imageSize.height);
                    //把图片加入到缓存
                    addBitmapToLruCache(path, bm);

                    //发送消息
                    refreshBitmap(bm, path, imageView);

                    //释放一个信号量
                    mPoolSemaphore.release();
                }
            });

        }


    }

    /**
     * 发送消息
     *
     * @param bm
     * @param path
     * @param imageView
     */
    private void refreshBitmap(Bitmap bm, String path, ImageView imageView) {
        Message msg = new Message();
        ImageBeanHolder holder = new ImageBeanHolder();
        holder.path = path;
        holder.bitmap = bm;
        holder.imageView = imageView;
        msg.obj = holder;
        //发送消息更新imageView的bitmap
        mMainHandler.sendMessage(msg);
    }

    /**
     * 将图片加入的缓存
     *
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCacheByPath(path) == null) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    /**
     * 图片压缩
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;//不把图片加载到内存中
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = caculateInsampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        return bm;
    }

    /**
     * 根据需求的宽高和实际的宽高
     *
     * @param options
     * @param reqwidth
     * @param reqheight
     * @return
     */
    private int caculateInsampleSize(BitmapFactory.Options options, int reqwidth, int reqheight) {
        int inSampleSize = 1;
        int width = options.outWidth;
        int height = options.outHeight;
        if (width > reqwidth || height > reqheight) {
            int widthRadio = Math.round(width * 1.0f / reqwidth);
            int heightRadio = Math.round(height * 1.0f / reqheight);
            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        return inSampleSize;
    }

    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize size = new ImageSize();
        DisplayMetrics dm = imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        int width = imageView.getWidth();
        if (width <= 0) {
            width = lp.width;
        }
        if (width <= 0) {
            width = getImageViewFiled(imageView,"mMaxWidth");
        }
        if (width <= 0) {
            width = dm.widthPixels;
        }
        int height = imageView.getHeight();
        if (height <= 0) {
            height = lp.height;
        }
        if (height <= 0) {
            height = getImageViewFiled(imageView,"mMaxHeight");
        }
        if (height <= 0) {
            height = dm.heightPixels;
        }
        size.width = width;
        size.height = height;
        return size;
    }

    /**
     * 通过反射获取ImageView的宽高
     * @param obj
     * @param filedName
     * @return
     */
    private int getImageViewFiled(Object obj,String filedName){

        int value = 0;
        try {
            Field  field = ImageView.class.getField(filedName);
            field.setAccessible(true);
            int filedValue = field.getInt(obj);
            if (filedValue > 0 && filedValue < Integer.MAX_VALUE){
                value = filedValue;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
     return value;
    }

    /**
     * 添加至任务队列
     * @param runnable
     */
    private synchronized void addTasks(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mPoolHandler == null) mPoolHandlerSemaphore.acquire();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolHandler.sendEmptyMessage(0x110);
    }

    /**
     * 通过path从缓存中取bitmap
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCacheByPath(String key) {
        return mLruCache.get(key);
    }

    private class ImageSize {
        private int width;
        private int height;
    }

    private class ImageBeanHolder {
        private String path;
        private Bitmap bitmap;
        private ImageView imageView;

    }
}

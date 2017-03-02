package com.hm.wechatimgloader;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hm.wechatimgloader.bean.FolderBean;
import com.hm.wechatimgloader.view.CustomPopWindow;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private GridView gridView;

    private TextView mDirName, mDirCount;
    private RelativeLayout mBottomLy;
    private List<String> mImgs;

    private File mCurrenDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<>();

    private ProgressDialog progressDialog;

    private static final int DATA_LOADED = 0X110;

    private CustomPopWindow popWindow;
    private GridViewAdapter mAdapter;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED) {
                progressDialog.dismiss();
                data2View();
                popWindow = new CustomPopWindow(MainActivity.this, mFolderBeans);
                initPopWindow();

            }
        }
    };

    private void initPopWindow() {
        popWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        popWindow.setOnItemClickListener(new CustomPopWindow.OnItemClickListener() {
            @Override
            public void onItemClick(FolderBean bean) {

                mCurrenDir = new File(bean.getDir());
                mImgs = Arrays.asList(mCurrenDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String fileName) {
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png"))
                            return true;
                        return false;
                    }
                }));
                mMaxCount = mImgs.size();
                mDirName.setText(bean.getDirName());
                mDirCount.setText(mMaxCount + "");

                mAdapter = new GridViewAdapter(MainActivity.this, mImgs, mCurrenDir.getAbsolutePath());
                gridView.setAdapter(mAdapter);

                popWindow.dismiss();
            }
        });
    }

    private void data2View() {

        if (mCurrenDir == null) {
            Toast.makeText(MainActivity.this, "未扫描到图片", Toast.LENGTH_SHORT).show();
            return;
        }

        mImgs = Arrays.asList(mCurrenDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String fileName) {
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png"))
                    return true;
                return false;
            }
        }));
        mDirName.setText(mCurrenDir.getName());
        mDirCount.setText(mMaxCount + "");

        mAdapter = new GridViewAdapter(MainActivity.this, mImgs, mCurrenDir.getAbsolutePath());
        gridView.setAdapter(mAdapter);

    }

    /**
     * 设置取消主界面阴影方法
     */
    private void lightOn() {

        WindowManager.LayoutParams lp = getWindow().getAttributes();

        lp.alpha = 1.0f;

        getWindow().setAttributes(lp);
    }

    /**
     * 设置主界面阴影方法
     */
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDatas();
        initEvent();
    }

    /**
     * 扫描存储卡图片
     */
    private void initDatas() {
        progressDialog = ProgressDialog.show(this, "", "正在扫描图片...");
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "存储卡不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread() {
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver contntResolver = MainActivity.this.getContentResolver();
                //扫描文件以jpeg或者png结尾的文件
                Cursor cs = contntResolver.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE + "=? or "
                        + MediaStore.Images.Media.MIME_TYPE + "=?", new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);
                Set<String> mDirPaths = new HashSet<String>();
                while (cs.moveToNext()) { //如果游标中有值
                    String path = cs.getString(cs.getColumnIndex(MediaStore.Images.Media.DATA)); //获取该文件夹下第一个文件
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null) continue; // 通过实践 父文件有不存在的可能 在这里避免
                    String dirPath = parentFile.getAbsolutePath();
                    FolderBean folderBean = null;
                    if (mDirPaths.contains(dirPath)) {  //如果目标被扫描过 就不再扫描了
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgPath(path);
                    }
                    if (parentFile.list() == null) continue;
                     //过滤掉其他类型的图片
                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String fileName) {
                            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png"))
                                return true;
                            return false;
                        }
                    }).length;
                    folderBean.setDirCount(picSize);
                    mFolderBeans.add(folderBean);

                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrenDir = parentFile;
                    }

                }
                cs.close();

                //扫描完成
                mHandler.sendEmptyMessage(DATA_LOADED);
            }
        }.start();

    }

    private void initEvent() {

        mBottomLy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popWindow.showAsDropDown(mBottomLy);
                lightOff();
            }
        });
    }


    private void initView() {
        gridView = (GridView) findViewById(R.id.id_gridView);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
        mBottomLy = (RelativeLayout) findViewById(R.id.id_bottom_layout);

    }
}

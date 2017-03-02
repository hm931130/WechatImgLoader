package com.hm.wechatimgloader.view;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.hm.wechatimgloader.MainActivity;
import com.hm.wechatimgloader.PicListAdapter;
import com.hm.wechatimgloader.R;
import com.hm.wechatimgloader.bean.FolderBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/2/22 0022.
 */

public class CustomPopWindow extends PopupWindow {


    private View contentView;

    private DisplayMetrics displayMetrics;

    private ListView listView;

    private PicListAdapter adapter;

    private int mWidth;
    private int mHeight;
    private List<FolderBean> mFolderBeans = new ArrayList<>();
    public CustomPopWindow(Context context, List<FolderBean> mbeans){
        this.mFolderBeans = mbeans;
        caculateWidthAndHeight(context);
        contentView = LayoutInflater.from(context).inflate(R.layout.layout_popwindow,null);
        setWidth(mWidth);
        setHeight(mHeight);
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setTouchInterceptor(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        setBackgroundDrawable(new BitmapDrawable());
        setAnimationStyle(R.style.PopWindowStyle);
        setContentView(contentView);
        initView(context);
    }

    private void caculateWidthAndHeight(Context context) {

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mWidth = dm.widthPixels;
        mHeight = (int) (dm.heightPixels * 0.75);

    }

    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener l){
        this.listener = l;
    }
   public interface OnItemClickListener{
       void onItemClick(FolderBean bean);
   }
    private void initView(Context context) {

        listView = (ListView) contentView.findViewById(R.id.id_pop_listview);
        adapter = new PicListAdapter(context,0,mFolderBeans);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (listener != null){
                    listener.onItemClick(mFolderBeans.get(position));
                }
            }
        });
    }
}

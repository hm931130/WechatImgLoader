package com.hm.wechatimgloader;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.hm.wechatimgloader.loder.ImageLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Administrator on 2017/2/22 0022.
 */

public class GridViewAdapter extends BaseAdapter {

    private List<String> mDatas = new ArrayList<>();
    private LayoutInflater mInflater;
    private String dirName;
    private static  final Set<String> mSelectedImg = new HashSet<>();
    public GridViewAdapter(Context context,List<String> listDatas,String dirName){
       this.mInflater = LayoutInflater.from(context);
        this.mDatas = listDatas;
        this.dirName = dirName;
    }
    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return mDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

       final ViewHolder holder;
        if (convertView == null){
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_gridview,null);
            holder.mPicImageView = (ImageView) convertView.findViewById(R.id.id_igv_imageview);
            holder.mImageButton = (ImageButton) convertView.findViewById(R.id.id_igv_imagebutton);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.mPicImageView.setImageResource(R.mipmap.pictures_no);

        //重置普通状态
        holder.mImageButton.setImageResource(R.mipmap.picture_unselected);
        holder.mPicImageView.setColorFilter(null);
        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(dirName+"/"+mDatas.get(position),holder.mPicImageView);
        final String filePath = dirName+"/"+ mDatas.get(position);
        holder.mPicImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View imageView) {
                if (mSelectedImg.contains(filePath)){ //已经选择过
                    mSelectedImg.remove(filePath);
                    holder.mPicImageView.setColorFilter(null);
                    holder.mImageButton.setImageResource(R.mipmap.picture_unselected);
                }else{
                    mSelectedImg.add(filePath);
                    holder.mPicImageView.setColorFilter(Color.parseColor("#9e000000"));
                    holder.mImageButton.setImageResource(R.mipmap.pictures_selected);
                }
            }
        });

        if (mSelectedImg.contains(filePath)){ //已经选择过
            holder.mPicImageView.setColorFilter(Color.parseColor("#9e000000"));
            holder.mImageButton.setImageResource(R.mipmap.pictures_selected);
        }
        return convertView;
    }

    private class ViewHolder{

        private ImageView mPicImageView;
        private ImageButton mImageButton;

    }

}

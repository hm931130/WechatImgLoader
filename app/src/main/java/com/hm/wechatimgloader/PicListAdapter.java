package com.hm.wechatimgloader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hm.wechatimgloader.bean.FolderBean;
import com.hm.wechatimgloader.loder.ImageLoader;

import java.util.List;

/**
 * Created by Administrator on 2017/2/22 0022.
 */

public class PicListAdapter extends ArrayAdapter<FolderBean> {


    private Context mContext;
    public PicListAdapter(Context context, int textViewResourceId, List<FolderBean> objects) {
        super(context, 0, objects);
        this.mContext = context;
    }
    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null){
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_pop,parent,false);
            holder.mPicImageView = (ImageView) convertView.findViewById(R.id.item_pop_imageView);
            holder.mDirName = (TextView) convertView.findViewById(R.id.item_pop_dirname);
            holder.mCount = (TextView) convertView.findViewById(R.id.item_pop_dircount);
            holder.mSelectedImageView = (ImageView) convertView.findViewById(R.id.item_pop_dirchoose);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }

        ImageLoader.getInstance().loadImage(getItem(position).getFirstImgPath(),holder.mPicImageView);
        holder.mDirName.setText(getItem(position).getDirName());
        holder.mCount.setText(getItem(position).getDirCount() +"");
        return convertView;
    }

    private class ViewHolder{

        private ImageView mPicImageView;
        private TextView mDirName;
        private TextView mCount;
        private ImageView mSelectedImageView;

    }
}

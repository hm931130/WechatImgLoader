package com.hm.wechatimgloader.bean;

/**
 * Created by Administrator on 2017/2/22 0022.
 */

public class FolderBean {

    /**
     * 当前文件夹的路径
     */
    private String dir;
    /**
     * 当前文件夹中第一张图片的路径
     */
    private String firstImgPath;
    /**
     * 当前文件夹的名称
     */
    private String dirName;
    /**
     * 当前文件夹中共有多少图片
     */
    private int dirCount;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int index = dir.lastIndexOf("/");
        this.dirName = dir.substring(index);
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getDirName() {
        return dirName;
    }


    public int getDirCount() {
        return dirCount;
    }

    public void setDirCount(int dirCount) {
        this.dirCount = dirCount;
    }
}

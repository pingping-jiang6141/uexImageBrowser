package org.zywx.wbpalmstar.plugin.ueximagebrowser;

public class PickMultiImageBean {

    //the first image of the group
    private String topImagePath;
    //the parent folder name
    private String folderName;
    //counts of the group
    private int imageCounts;

    public String getTopImagePath() {
        return topImagePath;
    }

    public void setTopImagePath(String topImagePath) {
        this.topImagePath = topImagePath;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public int getImageCounts() {
        return imageCounts;
    }

    public void setImageCounts(int imageCounts) {
        this.imageCounts = imageCounts;
    }

}
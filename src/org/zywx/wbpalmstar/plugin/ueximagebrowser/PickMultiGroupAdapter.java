package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import java.util.List;

import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.cache.ImageLoadTask;
import org.zywx.wbpalmstar.base.cache.ImageLoadTask$ImageLoadTaskCallback;
import org.zywx.wbpalmstar.base.cache.ImageLoaderManager;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class PickMultiGroupAdapter extends BaseAdapter{
    private List<PickMultiImageBean> list;
    private ListView mListView;
    protected LayoutInflater mInflater;
    private int selectItem = 0;
    private Activity mActivity;
    public ImageLoaderManager loaderManager;

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public PickMultiGroupAdapter(Activity activity, List<PickMultiImageBean> list, ListView listview){
        this.list = list;
        this.mListView = listview;
        this.mActivity = activity;
        loaderManager = ImageLoaderManager.initImageLoaderManager(activity);
        mInflater = LayoutInflater.from(activity);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        PickMultiImageBean mImageBean = list.get(position);
        ResoureFinder finder = ResoureFinder.getInstance();
        final String path = mImageBean.getTopImagePath();
        if(convertView == null){
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(finder.getLayoutId("plugin_imagebrowser_grid_group_item"), null);
            viewHolder.mImageView = (ImageView) convertView.findViewById(finder.getId("plugin_imagebrowser_group_image"));
            viewHolder.mTextViewTitle = (TextView) convertView.findViewById(finder.getId("plugin_imagebrowser_group_title"));
            viewHolder.mTextViewCounts = (TextView) convertView.findViewById(finder.getId("plugin_imagebrowser_group_count"));
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final Bitmap bitmap = loaderManager.getCacheBitmap(path);
        viewHolder.mImageView.setImageBitmap(bitmap);
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.srcUrl = path;
        if (bitmap == null) {
            loaderManager.asyncLoad(new GridImageLoadTask(imageInfo, path, mActivity)
                    .addCallback(new ImageLoadTask$ImageLoadTaskCallback() {
                        public void onImageLoaded(ImageLoadTask task, Bitmap bitmap) {
                            View tagedView = mListView.findViewWithTag(task.filePath);
                            if (tagedView != null) {
                                ((ImageView) tagedView).setImageBitmap(bitmap);
                            }
                        }
                    }));
        }
        viewHolder.mTextViewTitle.setText(mImageBean.getFolderName());
        viewHolder.mTextViewCounts.setText(Integer.toString(mImageBean.getImageCounts()) + "张");
        viewHolder.mImageView.setTag(path);
        if(position == selectItem){
            convertView.setBackgroundColor(Color.GRAY);
        }else{
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }
        return convertView;
    }

    public static class ViewHolder{
        public ImageView mImageView;
        public TextView mTextViewTitle;
        public TextView mTextViewCounts;
    }

    /**
     * current selected group
     * @param selectItem
     */
    public  void setSelectItem(int selectItem) {
        this.selectItem = selectItem;
    }

    public int getSelectItem() {
        return selectItem;
    }

}
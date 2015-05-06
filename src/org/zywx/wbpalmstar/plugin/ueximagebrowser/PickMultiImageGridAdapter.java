package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.cache.ImageLoadTask;
import org.zywx.wbpalmstar.base.cache.ImageLoadTask$ImageLoadTaskCallback;
import org.zywx.wbpalmstar.base.cache.ImageLoaderManager;
import org.zywx.wbpalmstar.plugin.ueximagebrowser.PickMultiActivity.MySelectedNumListener;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Toast;

public class PickMultiImageGridAdapter extends BaseAdapter {

    public static final String TAG = "ImageGridAdapter";

    public List<String> mList;
    public List<String> getList() {
        return mList;
    }

    /**
     * group changed
     * @param list
     */
    public void setList(List<String> list) {
        this.mList = list;
    }

    private LayoutInflater inflater;
    public ImageLoaderManager loaderManager;
    private GridView gridView;
    private Activity activity;
    private ResoureFinder finder;
    private FrameLayout.LayoutParams params;
    private MySelectedNumListener listener;
    public void setListener(MySelectedNumListener listener) {
        this.listener = listener;
    }
    private int maxCount;
    private HashMap<String, Boolean> mSelectMap;

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        //max selected count
        this.maxCount = maxCount;
    }

    public PickMultiImageGridAdapter(Activity activity, List<String> list, GridView gridView) {
        if (activity == null && list == null) {
            throw new NullPointerException("activity or list can not be null");
        }
        mSelectMap = new HashMap<String, Boolean>();
        finder = ResoureFinder.getInstance(activity);
        this.setList(list);
        inflater = LayoutInflater.from(activity);
        loaderManager = ImageLoaderManager.initImageLoaderManager(activity);
        this.gridView = gridView;
        this.activity = activity;
    }

    public int getCount() {
        return getList().size();
    }

    public String getItem(int position) {
        if (position >= 0 && position < getList().size()) {
            return getList().get(position);
        } else {
            return null;
        }
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewCache viewCache = null;
        if (convertView == null) {
            viewCache = new ViewCache();
            convertView = inflater.inflate(finder.getLayoutId("plugin_imagebrowser_grid_child_item"), null);
            viewCache.imageView = (ImageView) convertView.findViewById(finder
                    .getId("plugin_image_watcher_grid_item_image"));
            viewCache.imageView.setLayoutParams(params);
            viewCache.checkBox = (ImageView) convertView.findViewById(finder.getId("plugin_imagebrowser_child_checkbox"));
            convertView.setTag(viewCache);
        } else {
            viewCache = (ViewCache) convertView.getTag();
        }
        final String item = getItem(position);
        final Bitmap bitmap = loaderManager.getCacheBitmap(item);
        viewCache.imageView.setImageBitmap(bitmap);
        viewCache.imageView.setTag(item);
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.srcUrl = item;
        if (bitmap == null) {
            loaderManager.asyncLoad(new GridImageLoadTask(imageInfo, item, activity)
                    .addCallback(new ImageLoadTask$ImageLoadTaskCallback() {
                        public void onImageLoaded(ImageLoadTask task, Bitmap bitmap) {
                            View tagedView = gridView.findViewWithTag(task.filePath);
                            if (tagedView != null) {
                                ((ImageView) tagedView).setImageBitmap(bitmap);
                            }
                        }
                    }));
        }

        viewCache.checkBox.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                 boolean isCheck = mSelectMap.containsKey(item) && mSelectMap.get(item);
                 if((getSelectItems().size() == getMaxCount()) && !isCheck){
                     return;
                 }
                 mSelectMap.put(item, !isCheck);
                 updateCheckBox(!isCheck, (ImageView)arg0);
                 listener.onSelectedNumChanged(getSelectItems().size());
            }
        });
        boolean isCheck = mSelectMap.containsKey(item) ? mSelectMap.get(item) : false;
        updateCheckBox(isCheck, viewCache.checkBox);
        return convertView;
    }

    /**
     * update selected state
     * @param isCheck
     * @param view
     */
    private void updateCheckBox(boolean isCheck, ImageView view) {
        if(isCheck)
            view.setImageResource(finder.getDrawableId("plugin_imagebrowser_multi_selected"));
        else
            view.setImageResource(finder.getDrawableId("plugin_imagebrowser_multi_normal"));
    }

    public static class ViewCache {
        public ImageView imageView;
        public ImageView checkBox;
    }

    /**
     * get the selected items
     * @return image paths
     */
    public List<String> getSelectItems(){
        List<String> list = new ArrayList<String>();
        for(Iterator<Map.Entry<String, Boolean>> it = mSelectMap.entrySet().iterator(); it.hasNext();){
            Map.Entry<String, Boolean> entry = it.next();
            if(entry.getValue()){
                list.add(entry.getKey());
            }
        }
        return list;
    }

    public void setLayoutParams(LayoutParams params) {
        this.params = params;
    }
}
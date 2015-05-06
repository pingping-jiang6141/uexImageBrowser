package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import java.util.ArrayList;

import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.cache.ImageLoadTask;
import org.zywx.wbpalmstar.base.cache.ImageLoadTask$ImageLoadTaskCallback;
import org.zywx.wbpalmstar.base.cache.ImageLoaderManager;
import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageGridAdapter extends BaseAdapter {

	public static final String TAG = "ImageGridAdapter";

	public static ArrayList<ImageInfo> items = new ArrayList<ImageInfo>();
	private LayoutInflater inflater;
	public ImageLoaderManager loaderManager;
	private GridView gridView;
	private Activity activity;
	private ResoureFinder finder;

	public ImageGridAdapter(Activity activity, ArrayList<ImageInfo> list, GridView gridView) {
		if (activity == null && list == null) {
			throw new NullPointerException("activity or list can not be null........");
		}
		finder = ResoureFinder.getInstance(activity);
		items = syncCacheList(list);
		inflater = LayoutInflater.from(activity);
		loaderManager = ImageLoaderManager.initImageLoaderManager(activity);
		this.gridView = gridView;
		this.activity = activity;
	}

	public int getCount() {
		return items.size();
	}

	public static ArrayList<ImageInfo> getImageInfoList() {
		return items;
	}

	public static void reload(ArrayList<ImageInfo> list) {
		if (list == null) {
			throw new IllegalArgumentException("params can't be null....");
		}
		items.clear();
		items.addAll(list);
	}

	public static ArrayList<ImageInfo> syncCacheList(ArrayList<ImageInfo> list) {
		if (list != null) {
			for (int i = 0, size = list.size(); i < size; i++) {
				final ImageInfo newInfo = list.get(i);
				for (int j = 0, length = items.size(); j < length; j++) {
					final ImageInfo oldInfo = items.get(j);
					if (oldInfo.equals(newInfo)) {
						newInfo.savePath = oldInfo.savePath;
					}
				}
			}
		}
		return list;
	}

	public ImageInfo getItem(int position) {
		if (position >= 0 && position < items.size()) {
			return items.get(position);
		} else {
			return null;
		}
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		notifyDataSetChanged();
		ViewCache viewCache = null;
		if (convertView == null) {
			viewCache = new ViewCache();
			convertView = inflater.inflate(finder.getLayoutId("plugin_imagebrowser_grid_item_view"), null);
			viewCache.imageView = (ImageView) convertView.findViewById(finder
					.getId("plugin_image_watcher_grid_item_image"));
			convertView.setTag(viewCache);
		} else {
			viewCache = (ViewCache) convertView.getTag();
		}
		final ImageInfo item = items.get(position);
		final Bitmap bitmap = loaderManager.getCacheBitmap(item.srcUrl);
		viewCache.imageView.setImageBitmap(bitmap);
		viewCache.imageView.setTag(item.srcUrl);
		if (bitmap == null) {
			loaderManager.asyncLoad(new GridImageLoadTask(item, item.srcUrl, activity)
					.addCallback(new ImageLoadTask$ImageLoadTaskCallback() {

						@Override
						public void onImageLoaded(ImageLoadTask task, Bitmap bitmap) {
							View tagedView = gridView.findViewWithTag(task.filePath);
							if (tagedView != null) {
								((ImageView) tagedView).setImageBitmap(bitmap);
							}
						}
					}));
		}
		return convertView;
	}

	public static class ViewCache {
		public ImageView imageView;
	}

}
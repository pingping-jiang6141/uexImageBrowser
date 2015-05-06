package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import java.util.ArrayList;

import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.cache.ImageLoaderManager;
import org.zywx.wbpalmstar.base.cache.ImageLoaderManager$ImageLoadStatusListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;

public class ImageWatcherActivity extends Activity implements OnClickListener,
		OnItemClickListener, ImageLoaderManager$ImageLoadStatusListener {

	public static final String TAG = "ImageWatcherActivity";
	public static final int DIRECT_SHOW_TRUE = 1;
	public static final int DIRECT_SHOW_FALSE = 0;
	public static final int REQUEST_CODE_SHOW_PICTURE = 100;
	public static final String INTENT_KEY_DIRECT_SHOW = "direct_show";
	public static final String INTENT_KEY_SHOW_IMAGE_INDEX = "show_index";
	public static final String INTENT_KEY_URL_LIST = "urlList";
	public static final String FILE_PATH_SCHEMA = "file://";
	public static final String HTTP_PATH_SCHEMA = "http://";
	private GridView gridView;
	private ImageGridAdapter imageGridAdapter;
	private ProgressBar progressBar;
	private Button backBtn;
	private String strConfirm;
	private String strCancel;
	private String strPrompt;
	private ResoureFinder finder;
	private ArrayList<ImageInfo> arrayList = new ArrayList<ImageInfo>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		finder = ResoureFinder.getInstance(this);
		Intent intent = getIntent();
		final ArrayList<String> urlList = intent
				.getStringArrayListExtra(ImageWatcherActivity.INTENT_KEY_URL_LIST);
		if (urlList == null || urlList.size() == 0) {
			alertMessage(
					finder.getString("plugin_image_browser_undefine_image_url"),
					true);
			return;
		}
		for (String url : urlList) {
			final ImageInfo imageInfo = new ImageInfo(url, null);
			arrayList.add(imageInfo);
		}
		int showFlag = intent.getIntExtra(INTENT_KEY_DIRECT_SHOW, 0);
		if (showFlag == DIRECT_SHOW_TRUE) {
			Log.i(TAG, "Direct show true");
			ImageGridAdapter.reload(ImageGridAdapter.syncCacheList(arrayList));
			int index = intent.getIntExtra(
					ImageWatcherActivity.INTENT_KEY_SHOW_IMAGE_INDEX, 0);
			openPicture(arrayList, index);
			this.finish();
			return;
		}
		getWindow().getDecorView().setBackgroundDrawable(null);
		setContentView(finder.getLayoutId("plugin_imagebrowser_main_layout"));
		gridView = (GridView) findViewById(finder
				.getId("plugin_image_watcher_grid_list"));
		backBtn = (Button) findViewById(finder
				.getId("plugin_image_watcher_grid_top_back"));
		backBtn.setOnClickListener(this);
		progressBar = (ProgressBar) findViewById(finder
				.getId("plugin_image_watcher_main_top_loading"));
		imageGridAdapter = new ImageGridAdapter(this, arrayList, gridView);
		imageGridAdapter.loaderManager.setOnCountListener(this);
		gridView.setAdapter(imageGridAdapter);
		gridView.setOnItemClickListener(this);
		strCancel = finder.getString("cancel");
		strConfirm = finder.getString("confirm");
		strPrompt = finder.getString("prompt");
		startTimerTask();
	}
	
    private Handler handler = new Handler();  
    private static boolean isPending = false;
    private Runnable task = new Runnable() {  
        public void run() {                 
                isPending = false;
                try {
                	progressBar.setVisibility(View.GONE);
                	imageGridAdapter.notifyDataSetChanged();
                } catch (Exception e) {                        
                    e.printStackTrace();
                }            
        }  
    };  
    
	
	public void startTimerTask() {
		if (isPending == false) {
			isPending = true;
			handler.postDelayed(task, 10000);
		}
		
	}

	@Override
	public void onImageLoadStart(ImageLoaderManager manager) {
		progressBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void onImageLoadFinish(ImageLoaderManager manager) {
		progressBar.setVisibility(View.GONE);
	}

	public void onClick(View v) {
		if (v == backBtn) {
			this.finish();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		gridView.invalidate();
	}

	@Override
	protected void onDestroy() {
		if (imageGridAdapter != null) {
			imageGridAdapter.loaderManager.removeAllTask();
		}
		super.onDestroy();
	}

	private void openPicture(ArrayList<ImageInfo> infos, int index) {
		Intent intent = new Intent(this, PictureActivity.class);
		intent.putExtra(INTENT_KEY_SHOW_IMAGE_INDEX, index);
		startActivityForResult(intent, REQUEST_CODE_SHOW_PICTURE);
	}

	/**
	 * 点击九宫格item项
	 * 
	 * @param parent
	 * @param view
	 * @param postion
	 * @param id
	 */
	public void onItemClick(AdapterView<?> parent, View view, int postion,
			long id) {
		openPicture(arrayList, postion);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			confirmExit(
					strPrompt,
					finder.getString("plugin_image_browser_are_you_sure_to_exit_image_broswer"));
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == REQUEST_CODE_SHOW_PICTURE) {
			int index = data.getIntExtra(INTENT_KEY_SHOW_IMAGE_INDEX, 0);
			gridView.setSelection(index);
		}
	}

	private void confirmExit(String title, String message) {
		new AlertDialog.Builder(this)
				.setTitle(title)
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton(strConfirm,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								ImageWatcherActivity.this.finish();
							}
						}).setNegativeButton(strCancel, null).show();
	}

	private void alertMessage(String msg, final boolean exitOnClicked) {
		new AlertDialog.Builder(this)
				.setTitle(strPrompt)
				.setMessage(msg)
				.setCancelable(false)
				.setPositiveButton(strConfirm,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (exitOnClicked) {
									ImageWatcherActivity.this.finish();
								}
							}
						}).show();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

}
package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import org.zywx.wbpalmstar.base.BDebug;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;

/**
 * 加载大图片
 * 
 * @ClassName: ImageAsyncLoader
 * @Description: TODO
 * @author fangzhenyu
 * @date 2011-11-30 下午02:17:19
 */
public class ImageAsyncLoader extends AsyncTask<Object, Integer, Bitmap> {

	private String path;
	private ImageAsyncLoaderCallBack listener;
	private BitmapFactory.Options options = new BitmapFactory.Options();

	public ImageAsyncLoader(String path, ImageAsyncLoaderCallBack callBack) {
		this.path = path;
		this.listener = callBack;
	}

	@Override
	protected void onCancelled() {
		options.requestCancelDecode();
		if (listener != null) {
			listener.onCanceledLoad();
		}
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onStartLoadImage();
		}
	}

	@Override
	protected Bitmap doInBackground(Object... params) {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		Bitmap bitmap = null;
		try {
			int destSize = ImageUtility.getPictrueSourceMaxSize((Activity) params[0]);
			bitmap = ImageUtility.decodeSourceBitmapByPath(path, options, destSize);
		} catch (OutOfMemoryError e) {
			BDebug.log("OutOfMemoryError: " + e.getMessage());
		}
		return bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		Log.d("ImageAsyncLoader", "onPostExecute: " + Thread.currentThread().getName());
		if (listener != null) {
			listener.onImageLoaded(path, result);
		}
	}

	public static interface ImageAsyncLoaderCallBack {
		void onStartLoadImage();

		void onCanceledLoad();

		void onImageLoaded(String path, Bitmap bitmap);
	}

	public static class ImageAsyncLoaderCallBackAdapter implements ImageAsyncLoaderCallBack {

		@Override
		public void onCanceledLoad() {

		}

		@Override
		public void onImageLoaded(String path, Bitmap bitmap) {

		}

		@Override
		public void onStartLoadImage() {

		}
	}

}
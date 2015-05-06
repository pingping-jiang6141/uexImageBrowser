package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.zywx.wbpalmstar.base.BDebug;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.util.Log;

public class ImageUtility {

	public static final String TMP_FOLDER = "/sdcard/widgetone/tmp/";
	public static final String TAG = "ImageUtility";

	/**
	 * 需要加入的权限android.permission.ACCESS_NETWORK_STATE
	 */
	public static boolean isNetworkAvailable(Context context) {
		boolean isAvailable = false;
		ConnectivityManager cm = (ConnectivityManager) context
				.getApplicationContext().getSystemService(
						Context.CONNECTIVITY_SERVICE);
		if (cm != null) {
			NetworkInfo info = cm.getActiveNetworkInfo();
			if (info != null && info.isAvailable()) {
				isAvailable = true;
			}
		}
		BDebug.v(TAG, "isNetworkAvailable:" + isAvailable);
		return isAvailable;
	}

	public static Bitmap getPictureThumbnail(String filePath, int destSize)
			throws OutOfMemoryError {
		long start = System.currentTimeMillis();
		if (filePath == null || filePath.length() == 0) {
			return null;
		}
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, options);
		int height = options.outHeight;
		int width = options.outWidth;
		if (height <= 0 || width <= 0) {// 图片无法解码
			BDebug.d(TAG, "File:  " + filePath + "   can not be decode......");
			return null;
		}
		float scaleRate = 1;
		if (height > width) {
			scaleRate = height / destSize;
		} else {
			scaleRate = width / destSize;
		}
		options.inJustDecodeBounds = false;
		scaleRate = scaleRate > 1 ? scaleRate : 1;
		options.inSampleSize = (int) scaleRate;
		options.inJustDecodeBounds = false;
		options.inInputShareable = true;
		options.inPurgeable = true;
		options.inPreferredConfig = Config.ARGB_8888;// 会失真，缩略图失真没事^_^
		FileInputStream fis = null;
		Bitmap dest = null;
		try {
			fis = new FileInputStream(new File(filePath));
			Bitmap source = BitmapFactory.decodeFileDescriptor(fis.getFD(),
					null, options);// 再次解码，获得缩小后的图片
			if (source != null) {// 为了精确裁剪
				final float sacleRate = Math.max(
						(float) destSize / source.getWidth(), (float) destSize
								/ source.getHeight());
				final int destWidth = (int) (source.getWidth() * sacleRate);
				final int destHeight = (int) (source.getHeight() * sacleRate);
				dest = Bitmap.createScaledBitmap(source, destWidth, destHeight,
						false);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		String info = (dest == null) ? ""
				: ("  W: " + dest.getWidth() + " H:" + dest.getHeight());
		BDebug.i(
				TAG,
				"getThumbnail: " + info + " costTime:"
						+ (System.currentTimeMillis() - start) + "ms");
		return dest;
	}

	public static Bitmap loadTinyBitmapByLocalPath(String tinyPath)
			throws OutOfMemoryError {
		if (tinyPath == null || tinyPath.length() == 0) {
			return null;
		}
		final File file = new File(tinyPath);
		if (file == null || !file.exists() || file.isDirectory()) {
			return null;
		}
		FileInputStream fis = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		Bitmap bitmap = null;
		options.inInputShareable = true;
		options.inPurgeable = true;
		try {
			fis = new FileInputStream(file);
			bitmap = BitmapFactory.decodeStream(fis, null, options);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bitmap;
	}

	public static synchronized Bitmap decodeSourceBitmapByPath(String path,
			Options options, int maxSize) throws OutOfMemoryError {
		if (options.mCancel) {
			return null;
		}
		if (path == null || path.length() == 0) {
			return null;
		}
		final File file = new File(path);
		if (file == null || !file.exists() || file.isDirectory()) {
			return null;
		}
		if (options.mCancel) {
			return null;
		}
		Bitmap bitmap = null;
		options.inJustDecodeBounds = true;
		options.inSampleSize = 1;
		if (options.mCancel) {
			return null;
		}
		bitmap = BitmapFactory.decodeFile(path, options);
		int srcWidth = options.outWidth;
		int srcHeight = options.outHeight;
		if (srcHeight <= 0 || srcWidth <= 0) {// 无法解码
			Log.d(TAG,
					"decodeSourceBitmapByPath:   can not decode this bitmap!!!!!!!!!!! "
							+ path);
			return null;
		}
		float sampleRate = 1;
		if (srcHeight > srcWidth) {
			sampleRate = srcHeight / maxSize;
		} else {
			sampleRate = srcWidth / maxSize;
		}
		sampleRate = sampleRate > 1 ? sampleRate : 1;
		if (options.mCancel) {
			return null;
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			options.inSampleSize = (int) sampleRate;
			options.inJustDecodeBounds = false;
			options.inPurgeable = true;
			options.inInputShareable = true;
			options.inTempStorage = new byte[64 * 1024];
			// 使用JNI Heap,不占用VM Heap,避免大图片造成内存溢出
			if (!options.mCancel) {
				bitmap = BitmapFactory.decodeFileDescriptor(fis.getFD(), null,
						options);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (bitmap != null) {
			Log.i(TAG, "decodeSourceBitmapByPath:  W:" + bitmap.getWidth()
					+ " H:" + bitmap.getHeight() + " sampleRate:" + sampleRate);
		} else {
			Log.d(TAG,
					"decodeSourceBitmapByPath:   can not decode this bitmap!!!!!!!!!!!");
		}
		return bitmap;
	}

	public static Bitmap getScreenFitBitmapByPath(String path, Options options,
			int destSize) throws OutOfMemoryError {
		if (path == null || path.length() == 0) {
			return null;
		}
		final File file = new File(path);
		if (file == null || !file.exists() || file.isDirectory()) {
			return null;
		}
		Bitmap bitmap = null;
		options.inJustDecodeBounds = true;
		options.inSampleSize = 1;
		bitmap = BitmapFactory.decodeFile(path, options);
		int srcWidth = options.outWidth;
		int srcHeight = options.outHeight;
		if (srcHeight <= 0 || srcWidth <= 0) {// 无法解码
			return null;
		}
		int sampleRate = 1;
		if (srcHeight > srcWidth) {
			sampleRate = srcHeight / destSize;
		} else {
			sampleRate = srcWidth / destSize;
		}
		sampleRate = sampleRate > 1 ? sampleRate : 1;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			options.inSampleSize = sampleRate;
			options.inJustDecodeBounds = false;
			options.inPurgeable = true;
			options.inInputShareable = true;
			options.inPreferredConfig = Config.RGB_565;
			// 使用JNI Heap,不占用VM Heap,避免大图片造成内存溢出
			bitmap = BitmapFactory.decodeFileDescriptor(fis.getFD(), null,
					options);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (bitmap != null) {
			BDebug.i(TAG, "getScreenFitBitmapByPath:  W:" + bitmap.getWidth()
					+ " H:" + bitmap.getHeight() + " sampleRate:" + sampleRate);
		} else {
			BDebug.d(TAG,
					"getScreenFitBitmapByPath:   can not decode this bitmap!!!!!!!!!!!");
		}
		return bitmap;
	}

	/**
	 * 加载大图片
	 * 
	 * @param path
	 * @return
	 */
	public static Bitmap loadSrcBitmapByLocalPath(String path, Options options)
			throws OutOfMemoryError {
		if (path == null || path.length() == 0) {
			return null;
		}
		final File file = new File(path);
		if (file == null || !file.exists() || file.isDirectory()) {
			return null;
		}
		FileInputStream fis = null;
		Bitmap bitmap = null;
		try {
			fis = new FileInputStream(file);
			options.inJustDecodeBounds = false;
			options.inPurgeable = true;
			options.inInputShareable = true;
			options.inTempStorage = new byte[64 * 1024];
			// 使用JNI Heap,不占用VM Heap,避免大图片造成内存溢出
			bitmap = BitmapFactory.decodeFileDescriptor(fis.getFD(), null,
					options);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bitmap;
	}

	public static boolean writeNetDataToLocalFile(String url, String localPath) {
		int resCode = -1;
		InputStream is = null;
		FileOutputStream fos = null;
		File file = null;
		boolean isWrited = false;
		try {
			HttpGet httpGet = new HttpGet(url);
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(httpGet);
			resCode = response.getStatusLine().getStatusCode();
			if (resCode == HttpURLConnection.HTTP_OK) {
				is = response.getEntity().getContent();
				file = new File(localPath);
				fos = new FileOutputStream(file);
				byte[] buffer = new byte[4096];
				int actulSize = 0;
				while ((actulSize = is.read(buffer)) != -1) {
					fos.write(buffer, 0, actulSize);
				}
				isWrited = true;
			}
		} catch (IOException e) {
			BDebug.e(TAG, e.getMessage());
			e.printStackTrace();
		} catch (OutOfMemoryError error) {
			BDebug.e(TAG, "OutOfMemoryError:" + error.getMessage());
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (file != null && !isWrited) {
				file.delete();
			}
		}
		return isWrited;
	}

	public static byte[] readStreamToBytes(InputStream is) {
		if (is == null) {
			return null;
		}
		byte[] data = null;
		byte[] buffSize = new byte[8192];
		int actulSize = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			while ((actulSize = is.read(buffSize)) != -1) {
				baos.write(buffSize, 0, actulSize);
			}
			data = baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				baos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return data;
	}

	public static boolean writeBitmapToFile(Bitmap bitmap, File file) {
		if (bitmap == null || bitmap.isRecycled()) {
			return false;
		}
		if (file == null || file.isDirectory()) {
			return false;
		}
		boolean isWrited = false;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			isWrited = bitmap.compress(bitmap.hasAlpha() ? CompressFormat.PNG
					: CompressFormat.JPEG, 100, fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (!isWrited && file.exists()) {// 没写成功且文件存在,删除
				file.delete();
			}
		}
		return isWrited;
	}

	public static boolean writeDataToFile(byte[] data, String filePath) {
		if (data == null || filePath == null) {
			return false;
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		FileOutputStream fos = null;
		boolean isWrited = false;
		try {
			fos = new FileOutputStream(filePath);
			byte[] buffer = new byte[8192];
			int actualSize = 0;
			while ((actualSize = bais.read(buffer)) != -1) {
				fos.write(buffer, 0, actualSize);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bais != null) {
				try {
					bais.close();
					bais = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
					fos = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			isWrited = true;
		}
		return isWrited;
	}

	public static int getPictureThumbSize(Activity context) {
		final DisplayMetrics dm = context.getResources().getDisplayMetrics();
		switch (dm.densityDpi) {
		case DisplayMetrics.DENSITY_HIGH:
			return 135;
		case DisplayMetrics.DENSITY_LOW:
			return 68;
		case DisplayMetrics.DENSITY_MEDIUM:
			return 90;
		default:
			return 90;
		}
	}

	public static int getScreenFitSize(Activity activity) {
		return activity.getResources().getDisplayMetrics().widthPixels / 2;
	}

	public static int getPictrueSourceMaxSize(Activity context) {
		return context.getWindowManager().getDefaultDisplay().getHeight() * 2;
	}

	public static int[] getScreenPixels(Activity context) {
		final DisplayMetrics dm = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(dm);
		final int[] size = new int[2];
		size[0] = dm.widthPixels;
		size[1] = dm.heightPixels;
		return size;
	}

	public static File createRandomFileName(String imgUrl) {
		final File folder = new File(TMP_FOLDER);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		try {
			imgUrl = imgUrl.substring(imgUrl.lastIndexOf("/") + 1,
					imgUrl.lastIndexOf("."));
		} catch (Exception e1) {
			imgUrl = "imageCache_" + System.currentTimeMillis();
		}
		final File file = new File(folder, imgUrl + ".jpg");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}

	public static void deleteTmpFolder() {
		File folder = new File(TMP_FOLDER);
		try {
			if (folder.exists()) {
				File[] list = folder.listFiles();
				if (list != null && list.length > 0) {
					for (File file : list) {
						file.delete();
					}
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}
}
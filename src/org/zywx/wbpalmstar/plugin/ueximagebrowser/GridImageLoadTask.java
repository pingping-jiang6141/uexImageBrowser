package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.cache.BytesArrayFactory;
import org.zywx.wbpalmstar.base.cache.BytesArrayFactory$BytesArray;
import org.zywx.wbpalmstar.base.cache.ImageLoadTask;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

public class GridImageLoadTask extends ImageLoadTask {

	public static final String TAG = "GridImageLoadTask";

	private static final long serialVersionUID = -6832688551768110834L;
	public ImageInfo imageInfo;
	public int thumbSize;
	private Activity activity;

	public GridImageLoadTask(ImageInfo imageInfo, String filePath, Activity activity) {
		super(filePath);
		this.imageInfo = imageInfo;
		this.activity = activity;
		this.thumbSize = ImageUtility.getScreenFitSize(activity);
	}

	@Override
	public Bitmap doInBackground() throws OutOfMemoryError {
		Bitmap bitmap = null;
		if (imageInfo.savePath == null) {// 尚未把大图下载到本地
			if (imageInfo.srcUrl.startsWith(ImageWatcherActivity.HTTP_PATH_SCHEMA)) {// 网络路径
				final File savePath = ImageUtility.createRandomFileName(imageInfo.srcUrl);// 创建本地保存文件名
				if (ImageUtility.writeNetDataToLocalFile(imageInfo.srcUrl, savePath.getAbsolutePath())) {// 写入文件
					imageInfo.savePath = savePath.getAbsolutePath();// 保存成功
					FileInputStream is = null;
					try {
                        is = new FileInputStream(imageInfo.savePath);
                        if(is != null){
                            bitmap = BitmapFactory.decodeStream(is);
                        }
                        return bitmap;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
					
				}
			} else if (imageInfo.srcUrl.startsWith(BUtility.F_Widget_RES_SCHEMA)) {
				InputStream is = null;
				try {
					is = BUtility.getInputStreamByResPath(activity, imageInfo.srcUrl);
					bitmap = BitmapFactory.decodeStream(is);
					if (bitmap != null) {
						File destFile = ImageUtility.createRandomFileName(imageInfo.savePath);
						if (ImageUtility.writeBitmapToFile(bitmap, destFile)) {
							imageInfo.savePath = destFile.getAbsolutePath();
							return bitmap;
						}
					}
				} catch (Exception e) {
					BDebug.e(TAG, "doInBackground() ERROR:" + e.getMessage());
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}else if (imageInfo.srcUrl.startsWith(BUtility.F_Widget_RES_path)) {
				InputStream is = null;
                try {
                    is = activity.getAssets().open(imageInfo.srcUrl);
                    if (is != null) {
                        bitmap = BitmapFactory.decodeStream(is);
                    }
                    return bitmap;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
				imageInfo.savePath = imageInfo.srcUrl;
			}
		}
		if (imageInfo.savePath == null) { // 加载原始图片失败
			return null;
		}
		return ImageUtility.getPictureThumbnail(imageInfo.savePath, thumbSize);
	}

	@Override
	public BytesArrayFactory$BytesArray transBitmapToBytesArray(Bitmap bitmap) throws OutOfMemoryError {
		if (bitmap == null) {
			return null;
		}
		int size = bitmap.getWidth() * bitmap.getHeight() * 4;
		BytesArrayFactory$BytesArray bytesArray = BytesArrayFactory.getDefaultInstance().requestBytesArray(size);
		bitmap.compress(CompressFormat.JPEG, 100, bytesArray);
		return bytesArray;
	}

}
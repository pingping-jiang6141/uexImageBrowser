package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class UpdateMediaData {
	private static UpdateMediaData sUpdateMediaData = null;
	private Context mContext;

	public UpdateMediaData(Context context) {
		mContext = context;
	}

	public static UpdateMediaData getInstance(Context context) {
		if (sUpdateMediaData == null) {
			sUpdateMediaData = new UpdateMediaData(context);
		}
		sUpdateMediaData.mContext = context;
		return sUpdateMediaData;
	}

	/**
	 * 更新媒体库
	 * 
	 * @param path
	 *            要更新的文件或者目录
	 */
	public void updateFile(String path) {
		Log.i("uexImageBrowser", "插入媒体库");
		mContext.sendBroadcast(new Intent(
				Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"
						+ path)));
	}
}

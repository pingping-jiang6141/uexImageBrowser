package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.cache.MyAsyncTask;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.URLUtil;
import android.widget.Toast;

public class EUExImageBrowser extends EUExBase {

	public static final int F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK = 2;
	public static final int F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK_MULTI = 3;
	public static final int F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_CUT = 4;
	public static final String tag = "uexImageBrowser_";
	public static final String F_CALLBACK_NAME_IMAGE_BROWSER_PICK = "uexImageBrowser.cbPick";
	public static final String F_CALLBACK_NAME_IMAGE_BROWSER_SAVE = "uexImageBrowser.cbSave";
	public static final String IS_CROP_IMAGE = "isCropImage";
	public static final String OUTPUT_PATH = "outputPath";
	private static final String F_CALLBACK_NAME_IMAGE_BROWSER_CROPIMAGE = "uexImageBrowser.cbCropImage";
	private boolean isCropImage = false;

	private ResoureFinder finder;
	private Context mContext;

	public EUExImageBrowser(Context context, EBrowserView view) {
		super(context, view);
		mContext = context;
		finder = ResoureFinder.getInstance();
	}

	/**
	 * 从媒体库选择一张图片并返回路径
	 */
	public void pick(String[] params) {
		isCropImage = false;
		Intent intent = new Intent(mContext, ImageReviewActivity.class);
		intent.putExtra(IS_CROP_IMAGE, false);
		try {
			startActivityForResult(intent,
					F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void cropImage(String[] params){
		String outputPath = "wgt://";
		if (params != null && params.length > 0){
			try {
				JSONObject json = new JSONObject(params[0]);
				if (json.has(OUTPUT_PATH)){
					String str = json.getString(OUTPUT_PATH);
					if (!TextUtils.isEmpty(str)){
						outputPath = str;
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		String path = BUtility.makeRealPath(
				BUtility.makeUrl(mBrwView.getCurrentUrl(), outputPath),
				mBrwView.getCurrentWidget().m_widgetPath,
				mBrwView.getCurrentWidget().m_wgtType);
		if (TextUtils.isEmpty(path) || !path.startsWith("/")){
			errorCallback(0, 0, "error params!");
			return;
		}
		isCropImage = true;
		Intent intent = new Intent(mContext, ImageReviewActivity.class);
		intent.putExtra(IS_CROP_IMAGE, true);
		intent.putExtra(OUTPUT_PATH, path);
		try {
			startActivityForResult(intent,
					F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从媒体库选择多张图片并返回路径
	 */
	public void pickMulti(String[] params) {
		Intent intent = new Intent(mContext, PickMultiActivity.class);
		if (params.length > 0 && !TextUtils.isEmpty(params[0])
				&& !"0".equals(params[0])) {
			int maxCount = Integer.valueOf(params[0]);
			intent.putExtra("maxCount", maxCount);
		}
		try {
			startActivityForResult(intent,
					F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK_MULTI);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * uexImageBrowser. Open接口增加两个参数扩展为- open(String[] imageUrlSet, String
	 * activeIndex) activeIndex表示要第一个显示的图片下标
	 * 
	 * @param imageUrls
	 * @param activeIndex
	 */

	public void open(String[] params) {
		LogUtils.o("uexImageBrowser open() length: " + params.length);
		if (params.length < 1) {
			return;
		}
		String[] imageUrls = params[0].split(",");
		String activeIndex = null;
		String showFlag = "0";
		if (params.length > 1) {
			activeIndex = params[1];
			showFlag = "1";
		}
		if (imageUrls == null || imageUrls.length == 0) {
			errorCallback(
					0,
					EUExCallback.F_ERROR_CODE_IMAGE_BROWSER_OPEN_ARGUMENT_ERROR,
					finder.getString("plugin_image_browser_undefine_image_url"));
			return;
		}
		ArrayList<String> urlList = new ArrayList<String>();
		for (String itemPath : imageUrls) {
			String fullPath = BUtility.getFullPath(mBrwView.getCurrentUrl(),
					itemPath);
			if (BUtility.isSDcardPath(fullPath)) {
				fullPath = BUtility.getSDRealPath(fullPath,
						mBrwView.getCurrentWidget().m_widgetPath,
						mBrwView.getCurrentWidget().m_wgtType);
			}
			if (fullPath != null && fullPath.length() > 0) {
				try {
					fullPath = URLDecoder.decode(fullPath, "UTF-8");
					fullPath = fullPath.replace("/..", "").replace(
							BUtility.F_FILE_SCHEMA, "");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				urlList.add(fullPath);
			}
		}
		if (urlList.size() == 0) {
			errorCallback(
					0,
					EUExCallback.F_ERROR_CODE_IMAGE_BROWSER_OPEN_ARGUMENT_ERROR,
					finder.getString("plugin_image_browser_undefine_image_url"));
			return;
		}

		if (urlList.size() == 1) {
			Intent intent = new Intent(mContext, ImagePreviewActivity.class);
			intent.putExtra(ImagePreviewActivity.INTENT_KEY_IMAGE_URL,
					urlList.get(0));
			mContext.startActivity(intent);
		} else {
			Intent intent = new Intent(mContext, ImageWatcherActivity.class);
			int flag = 0;
			int index = 0;
			if (showFlag != null) {
				try {
					flag = Integer.parseInt(showFlag.trim());
				} catch (NumberFormatException e) {
					BDebug.e(tag, "open(): ERROR:" + e.getMessage());
				}
			}
			if (activeIndex != null) {
				try {
					index = Integer.parseInt(activeIndex.trim());
				} catch (NumberFormatException e) {
					BDebug.e(tag, "open(): ERROR:" + e.getMessage());
				}
			}
			intent.putExtra(ImageWatcherActivity.INTENT_KEY_DIRECT_SHOW, flag);
			intent.putExtra(ImageWatcherActivity.INTENT_KEY_SHOW_IMAGE_INDEX,
					index);
			intent.putStringArrayListExtra(
					ImageWatcherActivity.INTENT_KEY_URL_LIST, urlList);
			mContext.startActivity(intent);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK) {
			if (resultCode != Activity.RESULT_OK) {
				return;
			}
			String imagePath = data
					.getStringExtra(ImageReviewActivity.INTENT_KEY_PICK_IMAGE_RETURN_PATH);
			if (isCropImage){
				jsCallback(EUExImageBrowser.F_CALLBACK_NAME_IMAGE_BROWSER_CROPIMAGE, 0,
						EUExCallback.F_C_TEXT, imagePath);
			}else{
				jsCallback(EUExImageBrowser.F_CALLBACK_NAME_IMAGE_BROWSER_PICK, 0,
						EUExCallback.F_C_TEXT, imagePath);
			}
		} else if (requestCode == F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK_MULTI) {
			if (data == null) {
				return;
			}
			String[] paths = data.getStringArrayExtra("paths");
            String path = "";
            for (int i = 0; i < paths.length; i++) {
                path = path + paths[i] + ",";
            }
            path = path.substring(0, path.length() -1);
			jsCallback(EUExImageBrowser.F_CALLBACK_NAME_IMAGE_BROWSER_PICK, 0,
					EUExCallback.F_C_TEXT, path);
		}
	}

	@SuppressWarnings("unchecked")
	public void save(String[] params) {
		if (params.length < 1)
			return;
		String inPath = params[0];
		BDebug.d(tag, "save()  inPath: " + inPath);
		String fullPath = BUtility
				.getFullPath(mBrwView.getCurrentUrl(), inPath);
		if (BUtility.isSDcardPath(fullPath)) {
			fullPath = BUtility.getSDRealPath(fullPath,
					mBrwView.getCurrentWidget().m_widgetPath,
					mBrwView.getCurrentWidget().m_wgtType);// 只可能为file://协议或者res://协议
			try {
				fullPath = URLDecoder.decode(fullPath, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		if (fullPath == null || fullPath.length() == 0) {
			errorCallback(0,
					EUExCallback.F_ERROR_CODE_VIDEO_OPEN_ARGUMENTS_ERROR,
					finder.getString("path_error"));
			return;
		}
		final String finalPath = fullPath;
		BDebug.d(tag, "save() inFinalPath:" + finalPath);
		File destPath = null;
		try {
			destPath = new File(Environment.getExternalStorageDirectory(),
					"/DCIM/");
			if (!destPath.exists()) {
				destPath.mkdir();
			}
		} catch (Exception e) {
			BDebug.d(tag, "destPath:" + destPath);
		}
		BDebug.d(tag, "save() destPath:" + destPath);
		final File savePath = destPath;
		new MyAsyncTask() {

			public void handleOnPreLoad(MyAsyncTask task) {
				if (savePath == null) {
					Toast.makeText(
							mContext,
							finder.getString("plugin_image_browser_sd_have_not_mount_so_can_not_save"),
							Toast.LENGTH_SHORT).show();
					this.cancel(true);
				}
			};

			protected Object doInBackground(Object... params) {
				if (savePath == null) {
					return false;
				}
				boolean isSuc = false;
				File inFile = new File(finalPath.replace("file://", ""));
				if (URLUtil.isFileUrl(finalPath)) {// 本地路径
					FileInputStream fis = null;
					FileOutputStream fos = null;
					try {
						fis = new FileInputStream(inFile);
						byte[] buffer = new byte[8192];
						File outFile = new File(savePath.getAbsolutePath()
								+ "/" + inFile.getName());
						if (!outFile.exists()) {
							outFile.createNewFile();
						}
						fos = new FileOutputStream(outFile);
						int actualSize = -1;
						while ((actualSize = fis.read(buffer)) != -1) {
							fos.write(buffer, 0, actualSize);
						}
						isSuc = true;
						// 更新媒体库数据
						UpdateMediaData.getInstance(mContext).updateFile(
								outFile.getAbsolutePath());
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
						if (fos != null) {
							try {
								fos.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

					}

				} else if (URLUtil.isNetworkUrl(finalPath)) {// 网络路径
					isSuc = ImageUtility.writeNetDataToLocalFile(finalPath,
							inFile.getAbsolutePath() + "/" + inFile.getName());
				} else if (finalPath.startsWith(BUtility.F_Widget_RES_path)) {
					try {
						InputStream is = mContext.getAssets().open(finalPath);
						if (is != null) {
							FileOutputStream fos = null;
							try {
								byte[] buffer = new byte[8192];
								File outFile = new File(
										savePath.getAbsolutePath() + "/"
												+ inFile.getName());
								if (!outFile.exists()) {
									outFile.createNewFile();
								}
								fos = new FileOutputStream(outFile);
								int actualSize = -1;
								while ((actualSize = is.read(buffer)) != -1) {
									fos.write(buffer, 0, actualSize);
								}
								isSuc = true;
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								if (is != null) {
									try {
										is.close();
									} catch (IOException e) {

										e.printStackTrace();
									}
								}
								if (fos != null) {
									try {
										fos.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}

							}
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return isSuc;
			};

			public void handleOnCompleted(MyAsyncTask task, Object result) {
				boolean flag = (Boolean) result;
				if (flag) {
					Toast.makeText(
							mContext,
							finder.getString("plugin_image_browser_save_folder")
									+ savePath.getAbsolutePath(),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mContext,
							finder.getString("plugin_image_browser_save_fail"),
							Toast.LENGTH_SHORT).show();
				}
				jsCallback(F_CALLBACK_NAME_IMAGE_BROWSER_SAVE, 0,
						EUExCallback.F_C_INT, flag ? EUExCallback.F_C_SUCCESS
								: EUExCallback.F_C_FAILED);
			};

		}.execute(new Object[] {});

	}

	public void cleanCache(String[] params) {
		ImageUtility.deleteTmpFolder();
	}

	@Override
	protected boolean clean() {
		// TODO Auto-generated method stub
		return false;
	}
}

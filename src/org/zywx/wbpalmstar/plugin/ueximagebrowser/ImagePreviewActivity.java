package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.cache.MyAsyncTask;
import org.zywx.wbpalmstar.plugin.ueximagebrowser.MultiTouchImageView.OnSingleTapListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 修改了图片预览的逻辑，先保存图片到本地，再从本地进行二次采样后再读取，防止OOM
 * 
 * @modifiedAt 2014年9月18日 by yipeng.zhang
 */
public class ImagePreviewActivity extends Activity implements OnClickListener,
		OnSingleTapListener {

	public static final String INTENT_KEY_IMAGE_URL = "imageUrl";
	public static final String TAG = "ImagePreviewActivity";
	private Button btnBack;
	private Button btnSave;
	private RelativeLayout topLayer;
	private String imagePath;
	private MultiTouchImageView imageView;
	private AlphaAnimation fadeInAnim;
	private AlphaAnimation fadeOutAnim;
	private AlertDialog progressDialog;
	private TextView progressText;
	private MyAsyncTask localAsyncTask;
	private MyAsyncTask netAsyncTask;
	private File targetFile, tempFile;
	private String strPrompt;
	private ResoureFinder finder;
	boolean isPng = false;

	private void initProgressDialog() {
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(finder
				.getLayoutId("plugin_imagebrowser_progress_dialog_layout"),
				null);
		progressText = (TextView) view.findViewById(finder
				.getId("plugin_progress_dialog_text"));
		progressDialog = new AlertDialog.Builder(this).create();
		progressDialog.setView(view);
		progressDialog.setCancelable(true);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String tempPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/widgetone/tmp";
		tempFile = new File(tempPath);
		if (!tempFile.exists()) {
			tempFile.mkdirs();
		}
		tempFile = new File(tempPath + File.separator + UUID.randomUUID());
		if (tempFile.exists()) {
			tempFile.delete();
		}
		finder = ResoureFinder.getInstance(this);
		getWindow().getDecorView().setBackgroundDrawable(null);
		Intent intent = getIntent();
		if (intent == null
				|| (imagePath = intent.getStringExtra(INTENT_KEY_IMAGE_URL)) == null
				|| imagePath.length() == 0) {
			BUtility.alertMessage(
					this,
					strPrompt,
					finder.getString("plugin_image_browser_undefine_image_url"),
					true);
			return;
		}
		BDebug.i(TAG, "imagePath:" + imagePath);
		setContentView(finder.getLayoutId("plugin_imagebrowser_preview_layout"));
		topLayer = (RelativeLayout) findViewById(finder
				.getId("plugin_image_preview_top"));
		btnBack = (Button) findViewById(finder
				.getId("plugin_image_preview_btn_back"));
		btnSave = (Button) findViewById(finder
				.getId("plugin_image_preview_btn_save"));
		imageView = (MultiTouchImageView) findViewById(finder
				.getId("plugin_image_preview_photo"));
		btnBack.setOnClickListener(this);
		btnSave.setOnClickListener(this);
		topLayer.setOnClickListener(this);
		imageView.setOnSingleTapListener(this);

		LinearInterpolator interpolator = new LinearInterpolator();
		fadeInAnim = new AlphaAnimation(0.0f, 1.0f);
		fadeInAnim.setFillAfter(true);
		fadeInAnim.setDuration(300);
		fadeInAnim.setInterpolator(interpolator);
		fadeOutAnim = new AlphaAnimation(1.0f, 0.0f);
		fadeOutAnim.setFillAfter(true);
		fadeOutAnim.setDuration(300);
		fadeOutAnim.setInterpolator(interpolator);

		imagePath = imagePath.replace(BUtility.F_FILE_SCHEMA, "");
		initProgressDialog();
		LogUtils.o("imagePath==" + imagePath);
		if (URLUtil.isNetworkUrl(imagePath)) {// 是网络图片需要下载
			netAsyncTask = new MyAsyncTask() {
				private long totalSize = 0;
				private boolean countable = false;

				@SuppressWarnings("unchecked")
				@Override
				protected Object doInBackground(Object... params) {
					Bitmap bitmap = null;
					int resCode = -1;
					InputStream is = null;
					FileOutputStream fos = null;
					try {
						String downloadUrl = URLDecoder.decode(imagePath,
								"UTF-8");
						HttpGet httpGet = new HttpGet(downloadUrl);
						HttpClient httpClient = new DefaultHttpClient();
						HttpResponse response = httpClient.execute(httpGet);
						resCode = response.getStatusLine().getStatusCode();
						if (resCode == HttpURLConnection.HTTP_OK) {
							totalSize = response.getEntity().getContentLength();
							LogUtils.o("resCode==" + resCode + " totalSize== "
									+ totalSize);
							tempFile.createNewFile();
							if (totalSize > 0) {
								countable = true;
							}
							fos = new FileOutputStream(tempFile);
							is = response.getEntity().getContent();
							long downloaded = 0;
							byte[] buffer = new byte[4096];
							int actulSize = 0;
							int totalLength = 0;
							while ((actulSize = is.read(buffer)) != -1) {
								totalLength += actulSize;
								if (isCancelled()) {
									break;
								}
								fos.write(buffer, 0, actulSize);
								if (countable) {
									downloaded += actulSize;
									int percent = (int) (((double) downloaded / (double) totalSize) * 100);
									Integer[] percents = new Integer[] { percent };
									publishProgress(percents);
								}
							}
							fos.flush();
							LogUtils.o("ReadTotalLength==" + totalLength);

						}
					} catch (IOException e) {
						LogUtils.o("IOExceptionError: " + e.getMessage());
					} catch (OutOfMemoryError error) {
						LogUtils.o("OutOfMemoryError: " + error.getMessage());
					} finally {
						try {
							if (is != null) {
								is.close();
							}
						} catch (IOException e) {
						}
						if (fos != null) {
							try {
								fos.close();
							} catch (IOException e) {
							}
						}
					}
					if (!isCancelled()) {
						final int maxSize = ImageUtility
								.getPictrueSourceMaxSize(ImagePreviewActivity.this);
						bitmap = ImageUtility.decodeSourceBitmapByPath(
								tempFile.getAbsolutePath(), new Options(),
								maxSize);
					}
					return bitmap;
				}

				@Override
				public void handleOnPreLoad(final MyAsyncTask task) {
					progressText
							.setText(finder
									.getString("plugin_image_browser_now_loading_image"));
					progressDialog.show();
					progressDialog.setOnCancelListener(new OnCancelListener() {

						@Override
						public void onCancel(DialogInterface dialog) {
							if (task.cancel(false)) {
								Toast.makeText(
										ImagePreviewActivity.this,
										finder.getString("plugin_image_browser_download_is_canceled"),
										Toast.LENGTH_SHORT).show();
							}
						}
					});
				}

				@Override
				public void handleOnUpdateProgress(MyAsyncTask task, int percent) {
					progressText
							.setText(finder
									.getString("plugin_image_browser_now_loading_image")
									+ percent + "%");
				}

				@Override
				public void handleOnCanceled(MyAsyncTask task) {
					progressDialog.dismiss();
				}

				@Override
				public void handleOnCompleted(MyAsyncTask task, Object result) {
					progressDialog.dismiss();
					if (result != null) {
						Bitmap bitmap = (Bitmap) result;
						imageView.setImageBitmap(bitmap);
						Config config = bitmap.getConfig();
						if (config != null
								&& config.compareTo(Config.RGB_565) > 0) {
							isPng = true;
						} else {
							isPng = false;
						}
					} else {
						LogUtils.o("handleOnCompleted, bitmap result==null");
						Toast.makeText(
								ImagePreviewActivity.this,
								finder.getString("plugin_image_browser_load_image_fail"),
								Toast.LENGTH_SHORT).show();
					}
				}
			};
			netAsyncTask.execute(new Object[] {});
		} else {// 加载本地图片或者res协议图片
			final String path = imagePath;
			final int maxSize = ImageUtility.getPictrueSourceMaxSize(this);
			localAsyncTask = new MyAsyncTask() {
				private BitmapFactory.Options options = new BitmapFactory.Options();

				@Override
				protected Object doInBackground(Object... params) {
					Bitmap bitmap = null;
					InputStream is = null;
					FileOutputStream fos = null;
					try {
						if (path.startsWith(BUtility.F_Widget_RES_SCHEMA)) {
							is = BUtility.getInputStreamByResPath(
									ImagePreviewActivity.this, path);
						} else if (path.startsWith(BUtility.F_Widget_RES_path)) {// 检测widget/wgtRes/开头
							String resPath = path.replace(
									BUtility.F_Widget_RES_path,
									BUtility.F_Widget_RES_SCHEMA);
							is = BUtility.getInputStreamByResPath(
									ImagePreviewActivity.this, resPath);
						} else {
							is = new FileInputStream(new File(path));
						}
						// 此处改为写入本地临时文件，然后在后面通过系统的接口解析此文件来处理bitmap，从而防止直接解析流造成内存溢出问题
						fos = new FileOutputStream(tempFile);
						byte[] buffer = new byte[4096];
						int actulSize = 0;
						while ((actulSize = is.read(buffer)) != -1) {
							if (isCancelled()) {
								break;
							}
							fos.write(buffer, 0, actulSize);
						}
						fos.flush();

					} catch (OutOfMemoryError e) {
						LogUtils.o("OutOfMemoryError: " + e.getMessage());
					} catch (Exception e) {
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
					if (!isCancelled()) {
						bitmap = ImageUtility.decodeSourceBitmapByPath(
								tempFile.getAbsolutePath(), options, maxSize);
					}
					return bitmap;
				}

				public void handleOnPreLoad(MyAsyncTask task) {
					progressText
							.setText(finder
									.getString("plugin_image_browser_now_loading_image"));
					progressDialog.setCancelable(true);
					progressDialog.show();
				};

				public void handleOnCanceled(MyAsyncTask task) {
					progressDialog.dismiss();
					options.requestCancelDecode();
				};

				public void handleOnCompleted(MyAsyncTask task, Object result) {
					progressDialog.dismiss();
					Bitmap bitmap = (Bitmap) result;
					if (bitmap != null) {
						imageView.setImageBitmap(bitmap);
						Config config = bitmap.getConfig();
						if (config != null
								&& config.compareTo(Config.RGB_565) > 0) {
							isPng = true;
						} else {
							isPng = false;
						}
					} else {
						Toast.makeText(
								ImagePreviewActivity.this,
								finder.getString("plugin_image_browser_load_image_fail"),
								Toast.LENGTH_SHORT).show();
					}
				};

			};
			localAsyncTask.execute(new Object[] {});
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
				cancelAsyncTask();
			} else {
				this.finish();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void cancelAsyncTask() {
		if (localAsyncTask != null
				&& localAsyncTask.getStatus() != Status.FINISHED) {
			localAsyncTask.cancel(true);
		}
		if (netAsyncTask != null && netAsyncTask.getStatus() != Status.FINISHED) {
			netAsyncTask.cancel(true);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onClick(View v) {
		if (v == btnBack) {
			this.finish();
		} else if (v == btnSave) {
			if (tempFile != null) {
				new MyAsyncTask() {
					public void handleOnPreLoad(MyAsyncTask task) {
						if (Environment.getExternalStorageState().equals(
								Environment.MEDIA_MOUNTED)) {
							progressDialog.setCancelable(false);
							progressText
									.setText(finder
											.getString("plugin_image_borwser_now_saving_image_please_wait"));
							progressDialog.show();
						} else {
							cancel(true);
							Toast.makeText(
									ImagePreviewActivity.this,
									finder.getString("plugin_image_browser_sd_have_not_mount_so_can_not_save"),
									Toast.LENGTH_SHORT).show();
						}
					};

					protected Object doInBackground(Object... params) {
						boolean isSuc = false;
						final String targetPath = Environment
								.getExternalStorageDirectory()
								.getAbsolutePath()
								+ "/DCIM/";
						targetFile = new File(targetPath
								+ System.currentTimeMillis()
								+ (isPng ? ".png" : ".jpg"));
						FileInputStream fis = null;
						FileOutputStream fos = null;
						try {
							fis = new FileInputStream(tempFile);
							fos = new FileOutputStream(targetFile);
							byte[] buffer = new byte[4096];
							int len = -1;
							while ((len = fis.read(buffer, 0, buffer.length)) != -1) {
								fos.write(buffer, 0, len);
							}
							fos.flush();
							// 更新媒体库数据
							UpdateMediaData.getInstance(
									ImagePreviewActivity.this).updateFile(
									targetFile.getAbsolutePath());
							isSuc = true;
						} catch (Exception e) {
							LogUtils.o(e.getMessage());
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
						return isSuc;
					};

					public void handleOnCanceled(MyAsyncTask task) {
						progressDialog.dismiss();
					};

					public void handleOnCompleted(MyAsyncTask task,
							Object result) {
						progressDialog.dismiss();
						progressText.setText("");
						if ((Boolean) result) {
							Toast.makeText(
									ImagePreviewActivity.this,
									finder.getString("plugin_image_browser_save_folder")
											+ ": "
											+ targetFile.getAbsolutePath(),
									Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(
									ImagePreviewActivity.this,
									finder.getString("plugin_image_browser_save_fail"),
									Toast.LENGTH_SHORT).show();
						}
					};
				}.execute(new Object[] {});
			} else {
				Toast.makeText(
						ImagePreviewActivity.this,
						finder.getString("plugin_image_browser_image_have_not_load_can_not_save"),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void showTopLayer() {
		if (topLayer.getVisibility() != View.VISIBLE) {
			topLayer.setVisibility(View.VISIBLE);
			topLayer.startAnimation(fadeInAnim);
		}
	}

	private void hideTopLayer() {
		if (topLayer.getVisibility() == View.VISIBLE) {
			topLayer.setVisibility(View.INVISIBLE);
			topLayer.startAnimation(fadeOutAnim);
		}
	}

	@Override
	public void onSingleTap(MultiTouchImageView view) {
		if (topLayer.getVisibility() == View.VISIBLE) {
			hideTopLayer();
		} else {
			showTopLayer();
		}
	}
}
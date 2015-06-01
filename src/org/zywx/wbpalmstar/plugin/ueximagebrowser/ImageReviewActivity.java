package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.cache.MyAsyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageReviewActivity extends Activity implements OnClickListener {

	private ImageView imageView;
	private Button btnRePick;
	private Button btnConfirm;
	private String path;
	private boolean isPickSuccess = false;
	private ResoureFinder finder;
	public static final String INTENT_KEY_PICK_IMAGE_RETURN_PATH = "mediaImagePick";
	private boolean isCutSuccess = false;
	private boolean isCropImage = false;
	private String outputParent;
	private String outputPath;
    private ProgressDialog dialog=null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		finder = ResoureFinder.getInstance();
		startPickPhoto();
		isCropImage = getIntent().getBooleanExtra(EUExImageBrowser.IS_CROP_IMAGE, false);
		if (isCropImage){
			outputParent = getIntent().getStringExtra(EUExImageBrowser.OUTPUT_PATH);
			File parent = new File(outputParent);
			if (!parent.exists()){
				parent.mkdirs();
			}
		}
	}

	private void initViews() {
		setContentView(finder.getLayoutId("plugin_imagebrowser_review_layout"));
		getWindow().getDecorView().setBackgroundDrawable(null);
		imageView = (ImageView) findViewById(finder
				.getId("plugin_image_review_photo"));
		btnConfirm = (Button) findViewById(finder
				.getId("plugin_image_review_btn_confirm"));
		btnRePick = (Button) findViewById(finder
				.getId("plugin_image_review_btn_repick"));
		btnConfirm.setOnClickListener(this);
		btnRePick.setOnClickListener(this);
	}

	private void startPickPhoto() {
		isPickSuccess = false;
		Intent intent = null;
		if (Build.VERSION.SDK_INT == 16) {
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			intent.putExtra("return-data", true);
		} else {
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
		}
		try {
			startActivityForResult(
					intent,
					EUExImageBrowser.F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			this.finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EUExImageBrowser.F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK) {
			if (resultCode == Activity.RESULT_OK) {
				isPickSuccess = true;
				String url = data.getDataString();// "content://u5 903sfdj"
				if (url == null) {
					Toast.makeText(
							this,
							finder.getString("plugin_image_browser_system_have_not_return_image_path"),
							Toast.LENGTH_SHORT).show();
					finish();
					return;
				}
				if (URLUtil.isFileUrl(url)) {
					path = url.replace("file://", "");
				} else {
					Cursor c = managedQuery(data.getData(), null, null, null,
							null);
					boolean isExist = c.moveToFirst();
					if (isExist) {
						path = c.getString(c
								.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
					}
				}
				if (isCropImage){
					String fileName = path.substring(path.lastIndexOf(File.separator) + 1);
					if (outputParent.endsWith(File.separator)){
						outputPath = outputParent + fileName;
					}else{
						outputPath = outputParent + File.separator + fileName;
					}
					cropImageUri(data.getData(),300,300,
							EUExImageBrowser.F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_CUT,
							Uri.fromFile(new File(outputPath)));
				}else{
					loadResult();
				}
			} else {
				finish();
			}
		}else if (requestCode == EUExImageBrowser.F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_CUT){
			isCutSuccess = true;
			if (resultCode == Activity.RESULT_OK) {
                path = outputPath;
				loadResult();
			} else {
				loadResult();
			}
		}
	}

	/** 保存方法 */
	public void saveBitmap(String path,Bitmap bitmap) {
		File f = new File(path);
		if (f.exists()) {
			f.delete();
		}
		try {
			FileOutputStream out = new FileOutputStream(f);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}

	}

	private void loadResult(){
		initViews();
		BDebug.d("ImageReviewActivity", "path:" + path);
		final int degree=ImageUtil.readPictureDegree(path);
		final int maxSize = ImageUtility.getPictrueSourceMaxSize(this);
		new MyAsyncTask() {
			private BitmapFactory.Options options = new BitmapFactory.Options();

			@Override
			protected Object doInBackground(Object... params) {
				Bitmap bitmap = null;
				Bitmap newBitmap=null;
				try {
					bitmap = ImageUtility.decodeSourceBitmapByPath(
							path, options, maxSize);
					if (degree!=0) {
						newBitmap = ImageUtil.rotaingImageView(degree, bitmap);
						saveBitmap(path, newBitmap);
						ImageUtil.writePictureDegree(path);
						bitmap.recycle();
                        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{path}, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    @Override
                                    public void onScanCompleted(String path, Uri uri) {
                                    }
                                });
					}else{
						newBitmap=bitmap;
					}
					bitmap=null;
				} catch (OutOfMemoryError e) {
					LogUtils.e("ImageReviewActivity",
							"OutOfMemoryError: " + e.getMessage());
				}
      			return newBitmap;
			}

			public void handleOnCompleted(MyAsyncTask task,
										  Object result) {
                cancelProgress();
				if (result == null) {
					Toast.makeText(
							ImageReviewActivity.this,
							finder.getString("plugin_image_browser_load_image_fail"),
							Toast.LENGTH_SHORT).show();
				} else {
					Bitmap bitmap = (Bitmap) result;
					imageView.setImageBitmap(bitmap);
				}

			};
		}.execute(new Object[] {});
        showProgress();
	}

    public void showProgress(){
        if (isFinishing()){
            return;
        }
        dialog= ProgressDialog.show(this, "提示", "正在加载，请稍后...");
        dialog.setCancelable(true);
    }

    public void cancelProgress(){
        if (dialog!=null){
            dialog.dismiss();
        }
    }

	private void cropImageUri(Uri uri, int outputX, int outputY, int requestCode, Uri outPut) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("outputX", outputX);
		intent.putExtra("outputY", outputY);
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outPut);
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		startActivityForResult(intent, requestCode);
	}

	@Override
	public void onClick(View v) {
		if (v == btnRePick) {
			startPickPhoto();
		} else if (v == btnConfirm) {
			if ((isPickSuccess && !isCropImage) || (isCutSuccess && isCropImage)) {
				Intent intent = new Intent();
				intent.putExtra(INTENT_KEY_PICK_IMAGE_RETURN_PATH, path);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

}
package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.cache.MyAsyncTask;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

public class EUExImageBrowser extends EUExBase {

    private static final String BUNDLE_DATA = "data";
    private static final int MSG_PICK = 1;
    private static final int MSG_PICK_MULTI = 2;
    private static final int MSG_OPEN = 3;
    private static final int MSG_SAVE = 4;
    private static final int MSG_CLEAN_CACHE = 5;
    private static final int MSG_CROP_IMAGE = 6;
    public static final int F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK = 2;
    public static final int F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK_MULTI = 3;
    public static final int F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_CUT = 4;
    public static final String tag = "uexImageBrowser_";
    public static final String IS_CROP_IMAGE = "isCropImage";
    public static final String OUTPUT_PATH = "outputPath";

    private boolean isCropImage = false;

    private ResoureFinder finder;
    private Context mContext;

    public EUExImageBrowser(Context context, EBrowserView view) {
        super(context, view);
        mContext = context;
        finder = ResoureFinder.getInstance();
    }

    @Override
    protected boolean clean() {
        return false;
    }


    public void pick(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_PICK;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void pickMsg() {
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

    public void pickMulti(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_PICK_MULTI;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void pickMultiMsg(String[] params) {
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
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_OPEN;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void openMsg(String[] params) {
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
            String pre = "content://" + mContext.getPackageName() + ".sp";
            if (itemPath.startsWith(pre)){
                itemPath = itemPath.substring(pre.length());
            }
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

    public void save(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_SAVE;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void saveMsg(String[] params) {
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
                jsCallback(JsConst.CALLBACK_SAVE, 0,
                        EUExCallback.F_C_INT, flag ? EUExCallback.F_C_SUCCESS
                                : EUExCallback.F_C_FAILED);
            };

        }.execute(new Object[]{});
    }

    public void cleanCache(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_CLEAN_CACHE;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void cleanCacheMsg() {
        ImageUtility.deleteTmpFolder();
    }

    public void cropImage(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_CROP_IMAGE;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void cropImageMsg(String[] params) {
        String outputPath = "wgt://";
        if (params != null && params.length > 0){
            try {
                JSONObject json = new JSONObject(params[0]);
                if (json.has(OUTPUT_PATH)){
                    String str = json.getString(OUTPUT_PATH);
                    if (!TextUtils.isEmpty(str)) {
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

    @Override
    public void onHandleMessage(Message message) {
        if(message == null){
            return;
        }
        Bundle bundle=message.getData();
        switch (message.what) {

            case MSG_PICK:
                pickMsg();
                break;
            case MSG_PICK_MULTI:
                pickMultiMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_OPEN:
                openMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_SAVE:
                saveMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_CLEAN_CACHE:
                cleanCacheMsg();
                break;
            case MSG_CROP_IMAGE:
                cropImageMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            default:
                super.onHandleMessage(message);
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
                jsCallback(JsConst.CALLBACK_CROP_IMAGE, 0,
                        EUExCallback.F_C_TEXT, imagePath);
            }else{
                jsCallback(JsConst.CALLBACK_PICK, 0,
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
            jsCallback(JsConst.CALLBACK_PICK, 0,
                    EUExCallback.F_C_TEXT, path);
        }
    }

}

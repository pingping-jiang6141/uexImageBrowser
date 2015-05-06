package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.zywx.wbpalmstar.base.ResoureFinder;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PickMultiActivity extends Activity implements OnClickListener{

    private ResoureFinder finder;
    private TextView mSelectNum;//selected picture number
    private Button mBackBtn;
    private Button mSaveBtn;
    private Button mSelectGroup;
    private GridView mImageGridView;
    private ListView mGrouplst;
    private boolean mIsSelectOpen = false;
    private ProgressDialog mProgressDialog;
    private PickMultiImageGridAdapter mImageAdapter;
    private PickMultiGroupAdapter mGroupAdapter;
    private List<PickMultiImageBean> mGroupData;
    private HashMap<String, List<String>> mGruopMap = new HashMap<String, List<String>>();
    private final static int SCAN_OK = 1;//data loaded finish message
    private String[] mPaths;//selected image path
    private Animation openAnti, closeAnti;
    private int maxCount = -1;
    private int totalPic = 0;
    private MySelectedNumListener listener;//selected image number change listener
    private int itemX;//itemWidth
    private final static String ALL_PIC_TAG = "所有图片";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        maxCount = getIntent().getIntExtra("maxCount", -1);
        finder = ResoureFinder.getInstance();
        setContentView(finder.getLayoutId("plugin_imagebrowser_pick_multi_layout"));
        mSelectNum = (TextView) findViewById(finder.getId("plugin_image_pickmulti_txt_number")); 
        mBackBtn = (Button) findViewById(finder.getId("plugin_image_pickmulti_btn_back"));
        mSaveBtn = (Button) findViewById(finder.getId("plugin_image_pickmulti_btn_save"));
        mSelectGroup = (Button) findViewById(finder.getId("plugin_image_pickmulti_select_group_btn"));
        mGrouplst = (ListView) findViewById(finder.getId("plugin_image_pickmulti_group_lst"));
        mSaveBtn.setOnClickListener(this);
        mSaveBtn.setText("完成");
        mBackBtn.setOnClickListener(this);
        mSelectGroup.setOnClickListener(this);
        mImageGridView = (GridView) findViewById(finder.getId("plugin_image_pickmulti_image_grid"));
        WindowManager wm = (WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        itemX = (width-4)/3;
        mImageGridView.setVerticalSpacing(2);
        mImageGridView.setHorizontalSpacing(2);
        mImageGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                //onItemClick, preview Image
//                Intent intent = new Intent(PickMultiActivity.this, ImagePreviewActivity.class);
//                intent.putExtra(ImagePreviewActivity.INTENT_KEY_IMAGE_URL,
//                        (String)mImageAdapter.getItem(arg2));
//                PickMultiActivity.this.startActivity(intent);
            }
        });
        listener = new MySelectedNumListener();
        mGrouplst.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int position,
                    long id) {
                mGroupAdapter.setSelectItem(position);
                mGroupAdapter.notifyDataSetInvalidated();
                mSelectGroup.setText(mGroupData.get(position).getFolderName());
                hideGroupSelect();
                mImageAdapter.setList(mGruopMap.get(mGroupData.get(mGroupAdapter.getSelectItem()).getFolderName()));
                mImageAdapter.notifyDataSetChanged();
            }
        });
        getImages();
    }

    /**
     * update selected image number
     * @param num
     */
    private void updateSelectedNum(int num){
        if(num == 0){
            mSaveBtn.setEnabled(false);
            mSaveBtn.setTextColor(Color.GRAY);
        }else{
            mSaveBtn.setEnabled(true);
            mSaveBtn.setTextColor(Color.WHITE);
        }
        mSelectNum.setText(num + "/" + maxCount);
    }

    /**
     * hide select group view
     */
    private void hideGroupSelect() {
        mGrouplst.startAnimation(closeAnti);
        mGrouplst.setVisibility(View.GONE);
        mIsSelectOpen = false;
    }

    /**
     * get all images on device
     */
    private void getImages() {
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Toast.makeText(this, "暂无外部存储", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this, null, "正在加载...");
        new Thread(new Runnable() {
            public void run() {
                Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver mContentResolver = PickMultiActivity.this.getContentResolver();
                //the type is png or jpg
                Cursor mCursor = mContentResolver.query(mImageUri, null,
                        MediaStore.Images.Media.MIME_TYPE + "=? or "
                                + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[] { "image/jpeg", "image/png" }, MediaStore.Images.Media.DATE_MODIFIED + " DESC");
                totalPic = mCursor.getCount();
                while (mCursor.moveToNext()) {
                    //get image path
                    String path = mCursor.getString(mCursor
                            .getColumnIndex(MediaStore.Images.Media.DATA));
                    //get the parent folder name
                    String parentName = new File(path).getParentFile().getName();
                    if (!mGruopMap.containsKey(ALL_PIC_TAG)) {
                        List<String> chileList = new ArrayList<String>();
                        chileList.add(path);
                        mGruopMap.put(ALL_PIC_TAG, chileList);
                    } else {
                        mGruopMap.get(ALL_PIC_TAG).add(path);
                    }
                    //category storage depend on the parent folder name
                    if (!mGruopMap.containsKey(parentName)) {
                        List<String> chileList = new ArrayList<String>();
                        chileList.add(path);
                        mGruopMap.put(parentName, chileList);
                    } else {
                        mGruopMap.get(parentName).add(path);
                    }
                }
                mCursor.close();
                mGroupData = subGroupOfImage(mGruopMap);
                //send message when load finish
                mHandler.sendEmptyMessage(SCAN_OK);
            }
        }).start();
    }
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case SCAN_OK:
                mProgressDialog.dismiss();
                mGroupAdapter = new PickMultiGroupAdapter(PickMultiActivity.this, mGroupData, mGrouplst);
                mGrouplst.setAdapter(mGroupAdapter);
                mSelectGroup.setText(mGroupData.get(mGroupAdapter.getSelectItem()).getFolderName());
                mImageAdapter = new PickMultiImageGridAdapter(PickMultiActivity.this, 
                        mGruopMap.get(mGroupData.get(mGroupAdapter.getSelectItem()).getFolderName()), mImageGridView);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(itemX, itemX);
                mImageAdapter.setLayoutParams(params);
                mImageGridView.setAdapter(mImageAdapter);
                mImageAdapter.setMaxCount(maxCount);
                mImageAdapter.setListener(listener);
                if(maxCount <= 0){
                    //if the maxCount is not setted, default is all images count.
                    maxCount = totalPic;
                }
                updateSelectedNum(0);
                break;
            }
        }
    };

    /**
     * get group info
     * @param mGruopMap
     * @return datalist
     */
    private List<PickMultiImageBean> subGroupOfImage(HashMap<String, List<String>> mGruopMap){
        if(mGruopMap.size() == 0){
            return null;
        }
        List<PickMultiImageBean> list = new ArrayList<PickMultiImageBean>();
        PickMultiImageBean firstImage = new PickMultiImageBean();
        firstImage.setFolderName(ALL_PIC_TAG);
        firstImage.setImageCounts(mGruopMap.get(ALL_PIC_TAG).size());
        firstImage.setTopImagePath(mGruopMap.get(ALL_PIC_TAG).get(0));
        list.add(firstImage);
        Iterator<Map.Entry<String, List<String>>> it = mGruopMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<String>> entry = it.next();
            PickMultiImageBean mImageBean = new PickMultiImageBean();
            String key = entry.getKey();
            List<String> value = entry.getValue();
            mImageBean.setFolderName(key);
            mImageBean.setImageCounts(value.size());
            mImageBean.setTopImagePath(value.get(0));//the first image of the group
            if(!key.equals(ALL_PIC_TAG)){
                list.add(mImageBean);
            }
        }
        return list;
    }

    @Override
    public void onClick(View v) {
        if(v == mBackBtn){//back
            finish();
        }else if(v == mSaveBtn){//save
            List<String> selects = mImageAdapter.getSelectItems();
            mPaths = new String[selects.size()];
            for(int i = 0; i < mPaths.length; i++){
                mPaths[i] = selects.get(i);
            }
            Intent intent = new Intent();
            intent.putExtra("paths", mPaths);
            setResult(EUExImageBrowser.F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK_MULTI, intent);
            finish();
        }else if(v == mSelectGroup){//select group
            openAnti = AnimationUtils.loadAnimation(this,
                    finder.getAnimId("platform_imagebrowser_activity_open"));
            closeAnti = AnimationUtils.loadAnimation(this,
                    finder.getAnimId("platform_imagebrowser_activity_close"));
            if(!mIsSelectOpen){
                mGrouplst.startAnimation(openAnti);
                mGrouplst.setVisibility(View.VISIBLE);
                mIsSelectOpen = true;
            }else{
                hideGroupSelect();
            }
        }
    }

    private interface SelectedNumListener{
        public void onSelectedNumChanged(int num);
    }

    public class MySelectedNumListener implements SelectedNumListener{
        public void onSelectedNumChanged(int num) {
            updateSelectedNum(num);
        }
    }
}
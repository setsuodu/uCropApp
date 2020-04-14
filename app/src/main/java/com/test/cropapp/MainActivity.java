package com.test.cropapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.soundcloud.android.crop.Crop;

import java.io.File;

public class MainActivity extends Activity {
    private TextView resultText;
    private ImageView resultImage;
    private VideoView resultVideo;

    private static final String TAG = "[MainActivity]";
    private static final int REQUEST_PERMISSION = 100;
    private static final int REQUEST_GALLERY    = 101;
    private static final int REQUEST_Camera     = 102;
    private static final int REQUEST_CROP       = 103;
    private static final int REQUEST_VIDEO      = 104;
    private boolean hasPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultText  = (TextView) findViewById(R.id.textView);
        resultImage = (ImageView) findViewById(R.id.result_image);
        resultVideo = (VideoView) findViewById(R.id.result_video);

        Button galleryBtn = (Button) findViewById(R.id.gallery);
        galleryBtn.setOnClickListener(new GalleryClickListener()); //点击事件
        galleryBtn.setOnLongClickListener(new GalleryLongClickListener()); //长按事件

        Button videoBtn = (Button) findViewById(R.id.video);
        videoBtn.setOnClickListener(new VideoClickListener());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true;
            } else {
                Toast.makeText(this, "权限授予失败！", Toast.LENGTH_SHORT).show();
                hasPermission = false;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
//        super.onActivityResult(requestCode, resultCode, result);

        //path = "/storage/emulated/0/DCIM/Camera/IMG_20200331_191413.jpg"
        //Uri  = "content://com.android.providers.media.documents/document/video%3A224528"

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_GALLERY: {
                    Log.e(TAG, "onActivityResult: REQUEST_GALLERY: uri=" + result.getData()); //返回的是Uri
                    //onActivityResult: REQUEST_GALLERY: uri=content://media/external/images/media/221904
                    String path = getRealPathFromUri(this, result.getData());
                    resultText.setText("uri=" + result.getData() + "\n\npath=" + path);
                    break;
                } //相册
                case REQUEST_Camera: {
                    Log.e(TAG, "onActivityResult: REQUEST_Camera: uri=" + result.getData());
                    break;
                } //拍照
                case REQUEST_CROP: {
                    Log.e(TAG, "onActivityResult: REQUEST_CROP: uri=" + result.getData());
                    String path = getRealPathFromUri(this, result.getData());
                    Log.e(TAG, "onActivityResult: REQUEST_CROP: path=" + path);
                    break;
                } //裁剪
                case Crop.REQUEST_PICK: {
                    beginCrop(result.getData());
                    break;
                } //发起裁剪
                case Crop.REQUEST_CROP: {
                    handleCrop(resultCode, result);
                    break;
                } //裁剪结果
            }
        }
    }

    private class GalleryClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getId()==R.id.gallery) {
                Toast.makeText(MainActivity.this, "您点击了控件："+((TextView)v).getText(), Toast.LENGTH_SHORT).show();
                openGallery();
            }
        }
    }
    private class GalleryLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            if (v.getId()==R.id.gallery) {
                Toast.makeText(MainActivity.this, "您长按了控件："+((TextView)v).getText(), Toast.LENGTH_SHORT).show();
                onPick();
            }
            return false;
        }
    }

    private class VideoClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getId()==R.id.video) {
                Toast.makeText(MainActivity.this, "您点击了控件："+((TextView)v).getText(), Toast.LENGTH_SHORT).show();
                openVideo();
            }
        }
    }

    /**
     * 获取权限
     */
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查是否有存储和拍照权限
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            ) {
                hasPermission = true;
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_PERMISSION);
            }
        }
    }

    /**
     * 本地相册
     */
    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    /**
     * 选择视频
     */
    private void openVideo() {
        //Intent.ACTION_GET_CONTENT获取的是所有本地图片
        //Intent.ACTION_PICK获取的是相册中的图片
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        //intent.setType("image/*"); //选择图片
        // intent.setType("audio/*"); //选择音频
        intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）
        // intent.setType("video/*;image/*");//同时选择视频和图片

        /* 取得相片后返回本画面 */
        startActivityForResult(intent, REQUEST_VIDEO);
    }

    /**
     * 裁剪操作
     */
    public void onPick() {
        Crop.pickImage(this);
    }
    /**
     * 裁剪回调
     */
    private void beginCrop(Uri source) {
        int max=100,min=1;
        int rand = (int) (Math.random()*(max-min)+min);
        Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped" + rand)); //同一个uri写保护，无法覆盖
        Crop.of(source, destination).asSquare().start(this);
    }
    private void handleCrop(int resultCode, Intent result) {
        System.out.println("handleCrop: resultCode=" + resultCode);
        if (resultCode == RESULT_OK) {
            resultImage.setImageURI(Crop.getOutput(result));
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Path转Uri
    private static Uri getUriForFile(Context context, File file) {
        if (context == null || file == null) {
            throw new NullPointerException();
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(context.getApplicationContext(), "com.rain.takephotodemo.FileProvider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }
    // Uri转Path
    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}

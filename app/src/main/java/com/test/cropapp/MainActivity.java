package com.test.cropapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.soundcloud.android.crop.Crop;

import java.io.File;

public class MainActivity extends Activity {
    private ImageView resultImage;
    private VideoView resultVideo;

    final static int REQUEST_PERMISSION = 100;
    final static int REQUEST_GALLERY    = 101;
    final static int REQUEST_Camera     = 102;
    final static int REQUEST_CROP       = 103;
    final static int REQUEST_VIDEO      = 104;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultImage = (ImageView) findViewById(R.id.result_image);
        resultVideo = (VideoView) findViewById(R.id.result_video);

        Button galleryBtn = (Button) findViewById(R.id.gallery);
        galleryBtn.setOnClickListener(new GalleryClickListener()); //点击事件
        galleryBtn.setOnLongClickListener(new GalleryLongClickListener()); //长按事件

        Button videoBtn = (Button) findViewById(R.id.video);
        videoBtn.setOnClickListener(new VideoClickListener());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        } else if (requestCode == 1) {
            System.out.println("选择了视频 resultCode=" + resultCode + ", data=" + result.getData());
            //content://com.android.providers.media.documents/document/video%3A224528
        }
    }

    private class GalleryClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getId()==R.id.gallery) {
                Toast.makeText(MainActivity.this, "您点击了控件："+((TextView)v).getText(), Toast.LENGTH_SHORT).show();
                onPick();
            }
        }
    }
    private class GalleryLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            if (v.getId()==R.id.gallery) {
                Toast.makeText(MainActivity.this, "您长按了控件："+((TextView)v).getText(), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    private class VideoClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getId()==R.id.gallery) {
                Toast.makeText(MainActivity.this, "您点击了控件："+((TextView)v).getText(), Toast.LENGTH_SHORT).show();
                onPick();
            }
        }
    }

    /**
     * 本地相册
     */
    public void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    /**
     * 选择视频
     */
    private void chooseVideo() {
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
}

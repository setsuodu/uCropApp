package com.test.cropapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;

import java.io.File;

public class MainActivity extends Activity {
    private ImageView resultView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultView = (ImageView) findViewById(R.id.result_image);

        //按钮Button
        Button mButton = (Button) findViewById(R.id.button);
        //点击事件
        mButton.setOnClickListener(new MyOnClickListener());
        //长按事件
        mButton.setOnLongClickListener(new MyOnLongClickListener());
    }
    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getId()==R.id.button){
                Toast.makeText(MainActivity.this, "您点击了控件："+((TextView)v).getText(), Toast.LENGTH_SHORT).show();

                onPick();
            }
        }
    }
    private class MyOnLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            if (v.getId()==R.id.button){
                Toast.makeText(MainActivity.this, "您长按了控件："+((TextView)v).getText(), Toast.LENGTH_SHORT).show();
                chooseVideo();
            }
            return false;
        }
    }

    /**
     * 从相册中选择视频
     */
    private void choiceVideo() {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, 66);
    }
    private void chooseVideo() {
        Intent intent = new Intent();
        /* 开启Pictures画面Type设定为image */
        //intent.setType("image/*");
        // intent.setType("audio/*"); //选择音频
        intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）

        // intent.setType("video/*;image/*");//同时选择视频和图片

        /* 使用Intent.ACTION_GET_CONTENT这个Action */
        intent.setAction(Intent.ACTION_GET_CONTENT);
        /* 取得相片后返回本画面 */
        startActivityForResult(intent, 1);
    }

    public void onPick() {
        Crop.pickImage(this);
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
    private void beginCrop(Uri source) {
        int max=100,min=1;
        int ran2 = (int) (Math.random()*(max-min)+min);
        Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped" + ran2)); //同一个uri写保护，无法覆盖
        Crop.of(source, destination).asSquare().start(this);
    }
    private void handleCrop(int resultCode, Intent result) {
        System.out.println("handleCrop: resultCode=" + resultCode);
        if (resultCode == RESULT_OK) {
            resultView.setImageURI(Crop.getOutput(result));
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

package com.test.cropapp;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.Manifest;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
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

//import com.soundcloud.android.crop.Crop;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

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

        Button cameraBtn = (Button) findViewById(R.id.camera);
        cameraBtn.setOnClickListener(new CameraClickListener());

        Button videoBtn = (Button) findViewById(R.id.video);
        videoBtn.setOnClickListener(new VideoClickListener());

        checkPermissions();
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

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);

        //uri  = "content://media/external/images/media/210302"
        //path = "/storage/emulated/0/DCIM/Camera/IMG_20200331_191413.jpg"
        //uri  = "content://com.android.providers.media.documents/document/video%3A224527"

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_GALLERY: {
//                    Log.e(TAG, "onActivityResult: REQUEST_GALLERY: uri=" + result.getData()); //返回的是Uri

                    String path = toPath(data);
                    resultText.setText("uri=" + data.getData() + "\n\npath=" + path);
                    Log.e(TAG, "onActivityResult: REQUEST_GALLERY: path=" + path);
                    break;
                } //相册
                case REQUEST_Camera: {
                    Log.e(TAG, "onActivityResult: REQUEST_Camera: 拍照完成:" + imageUri);

                    String path = getRealFilePath(this, imageUri);
                    resultText.setText("uri=" + imageUri + "\n\npath=" + path);
                    Log.e(TAG, "onActivityResult: REQUEST_Camera: path=" + path);
                    break;
                } //拍照
                case REQUEST_CROP: {
//                    String path = toPath(data);
//                    resultText.setText("uri=" + data.getData() + "\n\npath=" + path);
//                    Log.e(TAG, "onActivityResult: REQUEST_CROP: path=" + path);
                    break;
                } //裁剪
                case REQUEST_VIDEO: {
                    String path = toPath(data);
                    resultText.setText("uri=" + data.getData() + "\n\npath=" + path);
                    Log.e(TAG, "onActivityResult: REQUEST_VIDEO: path=" + path);
                    break;
                } //选择视频
//                case Crop.REQUEST_PICK: {
//                    beginCrop(result.getData());
//                    break;
//                } //发起裁剪
//                case Crop.REQUEST_CROP: {
//                    handleCrop(resultCode, result);
//                    break;
//                } //裁剪结果
                case 1: {
                    final Uri selectedUri = data.getData();
                    if (selectedUri != null) {
                        startCrop(selectedUri);
                    } else {
                        Toast.makeText(MainActivity.this, "Cannot retrieve selected image", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case UCrop.REQUEST_CROP: {
                    handleCropResult(data);
                    break;
                }
            }
        }
        if (resultCode == UCrop.RESULT_ERROR) {
//            handleCropError(data);
            Log.e(TAG, "UCrop err: " + resultCode);
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
//                onPick();
                pickFromGallery();
            }
            return false;
        }
    }

    private class CameraClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getId()==R.id.camera) {
                Toast.makeText(MainActivity.this, "您点击了控件："+((TextView)v).getText(), Toast.LENGTH_SHORT).show();
                openCamera();
            }
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
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            ) {
                hasPermission = true;
            } else {
                requestPermissions(new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                },
                REQUEST_PERMISSION);
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
     * 拍照
     */
    Uri imageUri = null;
    public void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageUri = getImageUri();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, REQUEST_Camera);
    }
    public Uri getImageUri() {
        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        String path = file.getPath();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            imageUri = Uri.fromFile(file);
        } else {
            //兼容android7.0 使用共享文件的形式
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(MediaStore.Images.Media.DATA, path);
            imageUri = this.getApplication().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        }
        return imageUri;
    }

    /**
     * 选择视频
     */
    public void openVideo() {
        //Intent.ACTION_GET_CONTENT获取的是所有本地图片
        //Intent.ACTION_PICK获取的是相册中的图片
        Intent intent = new Intent(Intent.ACTION_PICK);
//        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        //intent.setType("image/*"); //选择图片
        // intent.setType("audio/*"); //选择音频
        intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）
        // intent.setType("video/*;image/*");//同时选择视频和图片

        /* 取得相片后返回本画面 */
        startActivityForResult(intent, REQUEST_VIDEO);
    }

//    /**
//     * soundcloud裁剪操作
//     */
//    public void onPick() {
//        Crop.pickImage(this);
//    }
//    /**
//     * soundcloud裁剪回调
//     */
//    private void beginCrop(Uri source) {
//        int max=100,min=1;
//        int rand = (int) (Math.random()*(max-min)+min);
//        Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped" + rand)); //同一个uri写保护，无法覆盖
//        Crop.of(source, destination).asSquare().start(this);
//    }
//    private void handleCrop(int resultCode, Intent result) {
//        System.out.println("handleCrop: resultCode=" + resultCode);
//        if (resultCode == RESULT_OK) {
//            resultImage.setImageURI(Crop.getOutput(result));
//        } else if (resultCode == Crop.RESULT_ERROR) {
//            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String[] mimeTypes = {"image/jpeg", "image/png"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }

        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

    private void startCrop(@NonNull Uri uri) {
        String destinationFileName = "SampleCropImage";
//        destinationFileName += ".png";
        destinationFileName += ".jpg";

        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));

        uCrop = basisConfig(uCrop);
        uCrop = advancedConfig(uCrop);

        uCrop.start(MainActivity.this);
    }

    /**
     * In most cases you need only to set crop aspect ration and max size for resulting image.
     *
     * @param uCrop - ucrop builder instance
     * @return - ucrop builder instance
     */
    private UCrop basisConfig(@NonNull UCrop uCrop) {
//        uCrop = uCrop.useSourceImageAspectRatio(); //radio_origin
//        uCrop = uCrop.withAspectRatio(1, 1); //radio_square
        // do nothing //radio_dynamic

//        int maxWidth = 512;
//        int maxHeight = 512;
//        uCrop = uCrop.withMaxResultSize(maxWidth, maxHeight);

        return uCrop;
    }

    /**
     * Sometimes you want to adjust more options, it's done via {@link com.yalantis.ucrop.UCrop.Options} class.
     *
     * @param uCrop - ucrop builder instance
     * @return - ucrop builder instance
     */
    private UCrop advancedConfig(@NonNull UCrop uCrop) {
        UCrop.Options options = new UCrop.Options();

//        options.setCompressionFormat(Bitmap.CompressFormat.PNG);
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);

        options.setCompressionQuality(90); //压缩

        options.setHideBottomControls(true);
        options.setFreeStyleCropEnabled(true);

        /*
        If you want to configure how gestures work for all UCropActivity tabs

        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        * */

        /*
        This sets max size for bitmap that will be decoded from source Uri.
        More size - more memory allocation, default implementation uses screen diagonal.

        options.setMaxBitmapSize(640);
        * */


       /*

        Tune everything (ﾉ◕ヮ◕)ﾉ*:･ﾟ✧

        options.setMaxScaleMultiplier(5);
        options.setImageToCropBoundsAnimDuration(666);
        options.setDimmedLayerColor(Color.CYAN);
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setCropGridStrokeWidth(20);
        options.setCropGridColor(Color.GREEN);
        options.setCropGridColumnCount(2);
        options.setCropGridRowCount(1);
        options.setToolbarCropDrawable(R.drawable.your_crop_icon);
        options.setToolbarCancelDrawable(R.drawable.your_cancel_icon);

        // Color palette
        options.setToolbarColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setRootViewBackgroundColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.your_color_res));

        // Aspect ratio options
        options.setAspectRatioOptions(1,
            new AspectRatio("WOW", 1, 2),
            new AspectRatio("MUCH", 3, 4),
            new AspectRatio("RATIO", CropImageView.DEFAULT_ASPECT_RATIO, CropImageView.DEFAULT_ASPECT_RATIO),
            new AspectRatio("SO", 16, 9),
            new AspectRatio("ASPECT", 1, 1));

       */

        return uCrop.withOptions(options);
    }

    private void handleCropResult(@NonNull Intent result) {
        final Uri resultUri = UCrop.getOutput(result);
        if (resultUri != null) {
            ResultActivity.startWithUri(MainActivity.this, resultUri);
        } else {
            Toast.makeText(MainActivity.this, R.string.toast_cannot_retrieve_cropped_image, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void handleCropError(@NonNull Intent result) {
        final Throwable cropError = UCrop.getError(result);
        if (cropError != null) {
            Log.e(TAG, "handleCropError: ", cropError);
            Toast.makeText(MainActivity.this, cropError.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, R.string.toast_unexpected_error, Toast.LENGTH_SHORT).show();
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
    public static String getRealFilePath( final Context context, final Uri uri ) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    private String toPath(Intent data) {
//        Log.e(TAG, "onActivityResult: SDK=" + Build.VERSION.SDK_INT); //SDK=29
        String path = "";
        if (Build.VERSION.SDK_INT >= 19) {
            path = handlePathOnKitKat(data);
        } else {
            path = handlePathBeforeKitKat(data);
        }
        return path;
    }
    private String handlePathBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String path = uri.getPath();
        //somethings
        return path;
    }
    @TargetApi(19)
    private String handlePathOnKitKat(Intent data) {
        String path = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                path = getPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                path = getPath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            path = getPath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }
        //somethins
        return path;
    }
    private String getPath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
}

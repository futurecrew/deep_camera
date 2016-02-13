package com.example.dj.deepcamera;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private Button capture, switchCamera;
    private ImageButton playButton;
    private ImageButton settingButton;
    private ImageButton playButton2;
    private ImageButton settingButton2;
    private Context myContext;
    private LinearLayout cameraPreview;
    private ImageView imageView;
    private boolean cameraFront = false;
    private FeedReaderDbHelper mDbHelper;
    private int mDbId;
    private String mServerIp;
    private String mServerPort;
    private String mImageSize;
    private String mMode;
    private String mCarType;
    private boolean mPlay;
    private final String TABLE_NAME = "setting_db";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.e("View", "onCreate() start");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;

        initialize();
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    public void onResume() {
        super.onResume();
        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        int cameraId = findFrontFacingCamera();

        //if (mCamera == null) {
            //if the front facing camera does not exist
            if (cameraId < 0) {
                Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                switchCamera.setVisibility(View.GONE);
            }
            if (mCamera == null) {
                mCamera = Camera.open(findBackFacingCamera());
                Log.e("View", "Camera.open() in onResume()");
            }
            mPreview.refreshCamera(mCamera);
        //}



        CameraInfo info =
                new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);

        if (degrees == 0 || degrees == 180) {
            findViewById(R.id.buttonsLayout1).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonsLayout2).setVisibility(View.INVISIBLE);
            imageView.setPadding(0, 0, 0, 100);
        } else {
            findViewById(R.id.buttonsLayout1).setVisibility(View.INVISIBLE);
            findViewById(R.id.buttonsLayout2).setVisibility(View.VISIBLE);
            imageView.setPadding(0, 0, 0, 0);
        }

    }

    public void initialize() {
        initializeDb();

        mCamera = Camera.open(findBackFacingCamera());

        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
        imageView = (ImageView) findViewById(R.id.imageView);

        mPreview = new CameraPreview(myContext, mCamera, imageView);
        cameraPreview.addView(mPreview);

        mPreview.setServer(mServerIp, mServerPort, mImageSize, mMode, mCarType);


        playButton = (ImageButton) findViewById(R.id.button_play);
        playButton.setOnClickListener(playButtonListener);

        settingButton = (ImageButton) findViewById(R.id.button_setting);
        settingButton.setOnClickListener(settingButtonListener);

        playButton2 = (ImageButton) findViewById(R.id.button_play2);
        playButton2.setOnClickListener(playButtonListener2);

        settingButton = (ImageButton) findViewById(R.id.button_setting2);
        settingButton.setOnClickListener(settingButtonListener2);

        mPlay = false;

        setPlayButton();
    }

    private void initializeDb() {
        mDbHelper = new FeedReaderDbHelper(this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                "_id",
                "server_ip",
                "server_port",
                "image_size",
                "mode",
                "car_type"
        };

        //db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "("
                + "_id integer primary key autoincrement, "
                + " server_ip text, "
                + " server_port text, "
                + " image_size text, "
                + " mode text, "
                + " car_type text)");

        Cursor c = db.query(TABLE_NAME, projection, null, null, null, null, null);
        boolean exist = c.moveToFirst();

        if (exist) {
            mDbId = c.getInt(c.getColumnIndex("_id"));
            mServerIp = c.getString(c.getColumnIndex("server_ip"));
            mServerPort = c.getString(c.getColumnIndex("server_port"));
            mImageSize = c.getString(c.getColumnIndex("image_size"));
            mMode = c.getString(c.getColumnIndex("mode"));
            mCarType = c.getString(c.getColumnIndex("car_type"));
        } else {
            mServerIp = "192.168.0.18";
            mServerPort = "8080";
            mImageSize = "800";
            mMode = "1";
            mCarType = "1";

            ContentValues values = new ContentValues();
            values.put("server_ip", mServerIp);
            values.put("server_port", mServerPort);
            values.put("image_size", mImageSize);
            values.put("mode", mMode);
            values.put("car_type", mCarType);

            mDbId = (int)db.insert(TABLE_NAME, null, values);
        }
    }

    void setPlayButton() {
        mPlay = !mPlay;
        Log.e("View", "playButtonListener mPlay : " + mPlay);

        if (mPlay) {
            playButton.setImageResource(R.drawable.stop);
            mPreview.setPlay(true);
            cameraPreview.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
        } else {
            playButton.setImageResource(R.drawable.play);
            mPreview.setPlay(false);
            cameraPreview.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.INVISIBLE);
        }
    }

    void setSettingButton() {
        Intent myIntent = new Intent(MainActivity.this, SettingActivity.class);
        myIntent.putExtra("server_ip", mServerIp);
        myIntent.putExtra("server_port", mServerPort);
        myIntent.putExtra("image_size", mImageSize);
        myIntent.putExtra("mode", mMode);
        myIntent.putExtra("car_type", mCarType);
        startActivityForResult(myIntent, 1);
    }

    OnClickListener playButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setPlayButton();
        }
    };

    OnClickListener playButtonListener2 = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setPlayButton();
        }
    };

    OnClickListener settingButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setSettingButton();
        }
    };

    OnClickListener settingButtonListener2 = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setSettingButton();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        {
            if(requestCode==1)
            {
                mServerIp = data.getStringExtra("server_ip");
                mServerPort = data.getStringExtra("server_port");
                mImageSize = data.getStringExtra("image_size");
                mMode = data.getStringExtra("mode");
                mCarType = data.getStringExtra("car_type");

                ContentValues values = new ContentValues();
                values.put("server_ip", mServerIp);
                values.put("server_port", mServerPort);
                values.put("image_size", mImageSize);
                values.put("mode", mMode);
                values.put("car_type", mCarType);

                SQLiteDatabase db = mDbHelper.getReadableDatabase();
                db.update(TABLE_NAME, values, "_id=?", new String[]{String.valueOf(mDbId)});

                mPreview.setServer(mServerIp, mServerPort, mImageSize, mMode, mCarType);
            }
        }
    }

    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mPreview.refreshCamera(mCamera);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Main", "onPause()");
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }

    private boolean hasCamera(Context context) {
        //check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    //make picture and save to a folder
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File("/sdcard/", "JCG Camera");

        //if this "JCGCamera folder does not exist
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        //take the current timeStamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        //and make a media file:
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
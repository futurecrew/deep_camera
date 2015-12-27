package com.example.dj.deepcamera;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private String img_folder;
    private List<Camera.Size> mSupportedPreviewSizes;
    //private Camera.Size mPreviewSize;
    //private Camera.Size mPictureSize;
    private Context mContext;
    private String mServerIp;
    private String mServerPort;
    private String mImageSize;
    private String mMode;
    int mPreviewWidth;
    int mPreviewHeight;
    int mPictureWidth;
    int mPictureHeight;
    int mDisplayWidth;
    int mDisplayHeight;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mContext = context;
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        img_folder = Environment.getExternalStorageDirectory().toString() + "/data/camera";
        File dir = new File(img_folder);
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }

        // supported preview sizes
        mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        for(Camera.Size str: mSupportedPreviewSizes)
            Log.e(VIEW_LOG_TAG, str.width + "/" + str.height);


        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay() ;

        int width = display.getWidth();
        int height = display.getHeight();

        Log.d(VIEW_LOG_TAG, "[display] width: " + width + ", height :" + height);

        Camera.Parameters parameters = mCamera.getParameters();
        if(parameters != null) {
            List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
            for (Camera.Size size : pictureSizeList) {
                Log.d(VIEW_LOG_TAG, "[picture] width: " + size.width + ", height :" + size.height);
            } //지원하는 사진의 크기


            List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
            for (Camera.Size size : previewSizeList) {
                Log.d(VIEW_LOG_TAG, "[preview] width: " + size.width + ", height :" + size.height);
            } //지원하는 프리뷰 크기
        }


        // preview : good, file : bad
        //mPreviewWidth = 1280;
        //mPreviewHeight = 800;

        // preview : good, file : good (1280x960)
        mPreviewWidth = 1280;
        mPreviewHeight = 960;

        // preview : stretched, file : good (1280x768)
        //mPreviewWidth = 1280;
        //mPreviewHeight = 768;

        // preview : good, file : good (640x480)
        //mPreviewWidth = 1080;
        //mPreviewHeight = 1920;

        mPictureWidth = mPreviewWidth;
        mPictureHeight = mPreviewHeight;

        mDisplayWidth = 768;
        mDisplayHeight = 1024;
    }

    public void setServer(String mServerIp, String mServerPort, String mImageSize, String mMode) {
        this.mServerIp = mServerIp;
        this.mServerPort = mServerPort;
        this.mImageSize = mImageSize;
        this.mMode = mMode;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // create the surface and start camera preview
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            Log.d(VIEW_LOG_TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void refreshCamera(Camera camera) {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            Log.e(VIEW_LOG_TAG, "preview surface does not exist");
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        setCamera(camera);
        try {
            Log.e(VIEW_LOG_TAG, "mPreviewSize = " + mPreviewWidth + "/" + mPreviewHeight);

            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();


            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
            parameters.setPictureSize(mPictureWidth, mPictureHeight);
            parameters.setFocusMode(parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(parameters);

            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera camera) {
                    try {
                        Camera.Parameters parameters = camera.getParameters();
                        Camera.Size previewSize = parameters.getPreviewSize();
                        Camera.Size pictureSize = parameters.getPictureSize();
                        YuvImage image = new YuvImage(data, parameters.getPreviewFormat(),
                                previewSize.width, previewSize.height, null);

                        Log.e(VIEW_LOG_TAG, "onPreviewFrame data.length = " + data.length + ", format = " + parameters.getPreviewFormat());
                        Log.e(VIEW_LOG_TAG, "onPreviewFrame previewSize = (" + previewSize.width + ", " + previewSize.height + ")");
                        Log.e(VIEW_LOG_TAG, "onPreviewFrame pictureSize = (" + pictureSize.width + ", " + pictureSize.height + ")");
                        Log.e(VIEW_LOG_TAG, "onPreviewFrame image = (" + image.getWidth() + ", " + image.getHeight() + ")");

                        File file = new File(img_folder, System.currentTimeMillis() + ".jpg"); // the File to save to
                        FileOutputStream filecon = new FileOutputStream(file);
                        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, filecon);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });


            /*
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera camera) {
                    //Log.d("MyCam", "setPreviewCallback");

                    //if (true)
                    //    return;

                    Camera.Parameters params = mCamera.getParameters();
                    //int w = params.getPreviewSize().width;
                    //int h = params.getPreviewSize().height;
                    int format = params.getPreviewFormat();
                    YuvImage image = new YuvImage(data, format, mPictureSize.width, mPictureSize.height, null);

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Rect area = new Rect(0, 0, mPictureSize.width, mPictureSize.height);
                    image.compressToJpeg(area, 50, out);
                    //Bitmap captureImg = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());


                    FileOutputStream fOut = null;
                    try {
                        File file = new File(img_folder, System.currentTimeMillis() + ".jpg"); // the File to save to
                        fOut = new FileOutputStream(file);
                        //ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        //captureImg.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                        //captureImg.compress(Bitmap.CompressFormat.JPEG, 85, bos); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                        //byte[] buffer = bos.toByteArray();
                        byte[] buffer = out.toByteArray();
                        fOut.write(buffer);
                        fOut.close();

                        String url = "http://" + mServerIp + ":" + mServerPort;

                        Log.d(VIEW_LOG_TAG, "calling ProcessHttpTask() mPictureSize.width:" + mPictureSize.width + ", mPictureSize.height:" + mPictureSize.height);

                        new ProcessHttpTask(url, mMode, buffer).execute();

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (fOut != null)
                                fOut.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }


                }
            });
            */

        } catch (Exception e) {
            Log.d(VIEW_LOG_TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        refreshCamera(mCamera);
    }

    public void setCamera(Camera camera) {
        //method to set a camera instance
        mCamera = camera;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        // mCamera.release();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        //int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        int temp = 0;

        if (mSupportedPreviewSizes != null) {
            final int rotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
            switch (rotation) {
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    temp = mDisplayWidth;
                    mDisplayWidth = mDisplayHeight;
                    mDisplayHeight = temp;
                    Log.d(VIEW_LOG_TAG, "ROTATION_90");
                    break;
            }
            setMeasuredDimension(mDisplayWidth, mDisplayHeight);
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        Log.d(VIEW_LOG_TAG, "getOptimalPreviewSize() w:" + w + ", h:" + h);

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}

//AsyncTask<Params,Progress,Result>
class ProcessHttpTask extends AsyncTask<Void, Void, Void> {
    private String url;
    private String mode;
    private byte[] data;

    public ProcessHttpTask(String url, String mode, byte[] data) {
        this.url = url;
        this.mode = mode;
        this.data = data;
    }

    @Override
    protected Void doInBackground(Void... params) {
        OutputStream outputToServer = null;

        //Log.d("View", "doInBackground");

        try {
            String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()

            HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "image/jpeg");
            //connection.setRequestProperty("Accept-Charset", charset);
            //connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);


            OutputStream os = connection.getOutputStream();
            os.write(data);
            os.flush();
            os.close();

            connection.connect();

            String response = "";
            int responseCode=connection.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else response = "";

            //Log.d("View", "response : " + response);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (outputToServer != null) {
                try {
                    outputToServer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String getQuery(List<Pair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Pair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode((String)pair.first, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode((String)pair.second, "UTF-8"));
        }

        return result.toString();
    }
}
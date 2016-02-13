package com.example.dj.deepcamera;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private ImageView mImageView;
    private String img_folder;
    private List<Camera.Size> mSupportedPreviewSizes;
    //private Camera.Size mPreviewSize;
    //private Camera.Size mPictureSize;
    private Context mContext;
    private String mServerIp;
    private String mServerPort;
    private String mImageSize;
    private String mMode;
    private String mCarType;
    private int carPlayFrameNo;
    private int rotationDegree;
    boolean play;
    int mPreviewWidth;
    int mPreviewHeight;
    int mPictureWidth;
    int mPictureHeight;
    int mDisplayWidth;
    int mDisplayHeight;

    public CameraPreview(Context context, Camera camera, ImageView imageView) {
        super(context);
        mContext = context;
        mCamera = camera;
        mImageView = imageView;
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        setImageFolder();

        carPlayFrameNo = 0;

        if (this.mMode != null && this.mMode.equals("3") == false) {
            File dir = new File(img_folder);
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    new File(dir, children[i]).delete();
                }
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

        //mPictureWidth = mPreviewWidth;
        //mPictureHeight = mPreviewHeight;

        mPictureWidth = 960;
        mPictureHeight = 1280;

        mDisplayWidth = 768;
        mDisplayHeight = 1024;
    }

    private void setImageFolder() {
        if (this.mMode != null && this.mMode.equals("3"))
            img_folder = Environment.getExternalStorageDirectory().toString() + "/Download/car/car" + this.mCarType;
        else
            img_folder = Environment.getExternalStorageDirectory().toString() + "/data/camera";

        Log.d(VIEW_LOG_TAG, "setImageFolder() img_folder = " + img_folder);
    }

    public void setServer(String mServerIp, String mServerPort, String mImageSize, String mMode, String mCarType) {
        this.mServerIp = mServerIp;
        this.mServerPort = mServerPort;
        this.mImageSize = mImageSize;
        this.mMode = mMode;
        this.mCarType = mCarType;

        setImageFolder();
    }

    public void setPlay(boolean play) {
        this.play = play;
        if (play == true)
            carPlayFrameNo = 0;
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

    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x -1)];
                i--;
            }
        }
        return yuv;
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

            final ExecutorService executor = Executors.newFixedThreadPool(10);

            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera camera) {
                    //Log.d("MyCam", "setPreviewCallback");

                    if (play == false)
                        return;

                    Camera.Parameters parameters = camera.getParameters();
                    Camera.Size previewSize = parameters.getPreviewSize();
                    Camera.Size pictureSize = parameters.getPictureSize();

                    YuvImage image = new YuvImage(data, parameters.getPreviewFormat(),
                            previewSize.width, previewSize.height, null);

                    if (rotationDegree == 0) {
                        byte[] yuvData = image.getYuvData();
                        byte[] newYuvData = rotateYUV420Degree90(yuvData, previewSize.width, previewSize.height);
                        image = new YuvImage(newYuvData, image.getYuvFormat(), image.getHeight(), image.getWidth(), null);
                    }

                    ByteArrayOutputStream out = new ByteArrayOutputStream();

                    float ratio = Float.parseFloat(mImageSize) / Math.max(image.getWidth(), image.getHeight());

                    Rect area = new Rect(0, 0, (int)(image.getWidth() * ratio), (int)(image.getHeight() * ratio));
                    image.compressToJpeg(area, 50, out);
                    //Bitmap captureImg = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());


                    FileOutputStream fOut = null;
                    String fileName = null;
                    byte[] buffer = null;
                    try {
                        if (mMode.equals("3")) {
                            carPlayFrameNo ++;
                            fileName = String.format("aa_%03d.jpg", carPlayFrameNo);
                            RandomAccessFile f = new RandomAccessFile(img_folder + "/" + fileName, "r");
                            buffer = new byte[(int)f.length()];
                            f.read(buffer);
                        } else {
                            fileName = System.currentTimeMillis() + ".jpg";
                            File file = new File(img_folder, fileName); // the File to save to
                            fOut = new FileOutputStream(file);
                            buffer = out.toByteArray();
                            fOut.write(buffer);
                            fOut.close();
                        }

                        String url = "http://" + mServerIp + ":" + mServerPort;

                        //Log.d(VIEW_LOG_TAG, "calling ProcessHttpTask() " + image.getWidth() + ", " + image.getHeight());

                        Runnable thread = new ProcessHttpTask(url, mMode, img_folder, fileName, buffer, mImageView);
                        executor.execute(thread);

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

        int newDisplayWidth = 0;
        int newDisplayHeight = 0;

        if (mSupportedPreviewSizes != null) {
            final int rotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
            switch (rotation) {
                case Surface.ROTATION_0:
                    rotationDegree = 0;
                    newDisplayWidth = mDisplayWidth;
                    newDisplayHeight = mDisplayHeight;
                    Log.d(VIEW_LOG_TAG, "ROTATION_0");
                    break;
                case Surface.ROTATION_90:
                    rotationDegree = 90;
                    newDisplayWidth = mDisplayHeight;
                    newDisplayHeight = mDisplayWidth;
                    Log.d(VIEW_LOG_TAG, "ROTATION_90");
                    break;
                case Surface.ROTATION_180:
                    rotationDegree = 180;
                    newDisplayWidth = mDisplayWidth;
                    newDisplayHeight = mDisplayHeight;
                    Log.d(VIEW_LOG_TAG, "ROTATION_180");
                    break;
                case Surface.ROTATION_270:
                    rotationDegree = 270;
                    newDisplayWidth = mDisplayHeight;
                    newDisplayHeight = mDisplayWidth;
                    Log.d(VIEW_LOG_TAG, "ROTATION_270");
                    break;
            }
            setMeasuredDimension(newDisplayWidth, newDisplayHeight);

            Log.d(VIEW_LOG_TAG, "onMeasure() w:" + newDisplayWidth + ", h:" + newDisplayHeight);
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

class ProcessHttpTask implements Runnable {
    private String url;
    private String mode;
    private String imageFolder;
    private String fileName;
    private byte[] data;
    private ImageView imageView;

    public ProcessHttpTask(String url, String mode, String imageFolder, String fileName, byte[] data, ImageView imageView) {
        this.url = url;
        this.mode = mode;
        this.imageFolder = imageFolder;
        this.fileName = fileName;
        this.data = data;
        this.imageView = imageView;
    }

    @Override
    public void run() {
        OutputStream outputToServer = null;

        //Log.d("View", "doInBackground");

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        String Tag="fSnd";

        try {

            String urlParameters  = "param1=a&param2=b&param3=c";
            byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
            int    postDataLength = postData.length;
            //String request        = "http://www.google.com";
            //String request        = "http://192.168.0.18:8080/";
            URL    url            = new URL( this.url );
            HttpURLConnection conn= (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            //conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"name\"" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(this.fileName);
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + lineEnd);

            dos.writeBytes("Content-Disposition: form-data; name=\"mode\""+ lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(this.mode);
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + lineEnd);

            dos.writeBytes("Content-Disposition: form-data; name=\"data\";filename=\"" + this.fileName +"\"" + lineEnd);
            dos.writeBytes(lineEnd);

            //Log.e(Tag,"Headers are written");

            dos.write(this.data);

            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            dos.flush();





            InputStream is      = null;
            BufferedReader in   = null;
            String data         = "";

            is  = conn.getInputStream();
            in  = new BufferedReader(new InputStreamReader(is), 8 * 1024);

            String line = null;
            StringBuffer buff   = new StringBuffer();

            while ( ( line = in.readLine() ) != null )
            {
                buff.append(line + "\n");
            }
            data = buff.toString().trim();

            /*
            Log.d("View", "data : " + data);
            Log.d("View", "imageView.getVisibility() : " + imageView.getVisibility());
            Log.d("View", "file : " + imageFolder + "/" + fileName);
            */

            //imageFolder = "data/camera";
            //Bitmap myBitmap = BitmapFactory.decodeFile("/sdcard/" + imageFolder + "/" + fileName);
            //imageView.setImageBitmap(myBitmap);
            //imageView.setImageDrawable(Drawable.createFromPath(imageFolder + '/' + fileName));

            /*
            if (Math.random() > 0.5)
                //imageView.setImageDrawable(Drawable.createFromPath("/mnt/sdcard/DCIM/100LGDSC/1423365220314.jpeg"));
                new BitmapWorkerTask(imageView, "/mnt/sdcard/DCIM/100LGDSC/1423365220314.jpeg").execute();
            else
                //imageView.setImageDrawable(Drawable.createFromPath("/mnt/sdcard/DCIM/100LGDSC/1420536102739.jpeg"));
                new BitmapWorkerTask(imageView, "/mnt/sdcard/DCIM/100LGDSC/1420536102739.jpeg").execute();
            */
            new BitmapWorkerTask(data, imageView, imageFolder + "/" + fileName).execute();
            //imageView.setImageDrawable(Drawable.createFromPath("/mnt/sdcard/" + imageFolder + "/" + fileName));

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

class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
    private final WeakReference<ImageView> imageViewReference;
    private int data = 0;
    private String jsonData;
    private String filePath;

    private int getColor(String key) {
        if (key.equals("1"))
            return Color.BLUE;
        else if (key.equals("2"))
            return Color.RED;
        else if (key.equals("3"))
            return Color.GRAY;
        else if (key.equals("4"))
            return Color.BLACK;
        else if (key.equals("5"))
            return Color.rgb(0xA5, 0x2a, 0x2a);
        else if (key.equals("6"))
            return Color.CYAN;
        else if (key.equals("7"))
            return Color.GREEN;
        else if (key.equals("8"))
            return Color.MAGENTA;
        else if (key.equals("9"))
            return Color.rgb(0xff, 0xa5, 0x00);
        else if (key.equals("10"))
            return Color.rgb(0x7f, 0xff, 0xd4);
        else if (key.equals("11"))
            return Color.rgb(0xff, 0xc0, 0xcb);
        else if (key.equals("12"))
            return Color.rgb(0x80, 0x00, 0x80);
        else if (key.equals("13"))
            return Color.rgb(0xee, 0x82, 0xee);
        else if (key.equals("14"))
            return Color.rgb(0xdd, 0xa0, 0xdd);
        else if (key.equals("15"))
            return Color.YELLOW;
        else if (key.equals("16"))
            return Color.rgb(0xff, 0xd7, 0x00);
        else if (key.equals("17"))
            return Color.rgb(0xf0, 0xe6, 0x8c);
        else if (key.equals("18"))
            return Color.rgb(0x00, 0x00, 0x80);
        else if (key.equals("19"))
            return Color.rgb(0xda, 0x70, 0xd6);
        else if (key.equals("20"))
            return Color.rgb(0xff, 0x7f, 0x50);
        else
            return Color.WHITE;
    }

    private String getClassText(String key) {
        if (key.equals("1"))
            return "aeroplane";
        else if (key.equals("2"))
            return "bicycle";
        else if (key.equals("3"))
            return "bird";
        else if (key.equals("4"))
            return "boat";
        else if (key.equals("5"))
            return "bottle";
        else if (key.equals("6"))
            return "bus";
        else if (key.equals("7"))
            return "car";
        else if (key.equals("8"))
            return "cat";
        else if (key.equals("9"))
            return "chair";
        else if (key.equals("10"))
            return "cow";
        else if (key.equals("11"))
            return "diningtable";
        else if (key.equals("12"))
            return "dog";
        else if (key.equals("13"))
            return "horse";
        else if (key.equals("14"))
            return "motorbike";
        else if (key.equals("15"))
            return "person";
        else if (key.equals("16"))
            return "pottedplant";
        else if (key.equals("17"))
            return "sheep";
        else if (key.equals("18"))
            return "sofa";
        else if (key.equals("19"))
            return "train";
        else if (key.equals("20"))
            return "tvmonitor";
        return "NA";
    }

    public BitmapWorkerTask(String jsonData, ImageView imageView, String filePath) {
        this.jsonData = jsonData;
        this.filePath = filePath;
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<ImageView>(imageView);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(Integer... params) {
        return BitmapFactory.decodeFile(filePath);
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        Log.d("View", "jsonData : " + jsonData);
        if (jsonData == null || jsonData.equals("NULL RESPONSE") == true)
            return;

        if (imageViewReference != null && bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                Paint paint = new Paint();

                Bitmap myBitmap = BitmapFactory.decodeFile(filePath);
                if (myBitmap == null)
                    return;

                if (jsonData.equals("{}") == true) {
                    imageView.setImageBitmap(myBitmap);
                    return;
                }


                Bitmap newBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
                Canvas tempCanvas = new Canvas(newBitmap);

                //Draw the image bitmap into the cavas
                tempCanvas.drawBitmap(myBitmap, 0, 0, null);

                try {
                    JSONObject jsonRootObject = new JSONObject(jsonData);

                    Iterator<String> iter = jsonRootObject.keys();

                    while (iter.hasNext()) {
                        String key = iter.next();
                        JSONArray jsonArray = jsonRootObject.getJSONArray(key);

                        int classColor = getColor(key);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONArray coords = jsonArray.getJSONArray(i);

                            paint.setStyle(Paint.Style.FILL);
                            paint.setColor(Color.WHITE);
                            tempCanvas.drawRect(coords.getInt(0), coords.getInt(1) - 20, coords.getInt(0) + 150, coords.getInt(1), paint);
                            paint.setColor(Color.BLACK);
                            paint.setTextSize(20);
                            tempCanvas.drawText(getClassText(key), coords.getInt(0), coords.getInt(1) - 5, paint);

                            //Log.d("View", "drawText() : " + getClassText(key) + ", (" + coords.getInt(0) + ", " + (coords.getInt(1) - 5) + ")");

                            paint.setStrokeWidth(5);
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setColor(classColor);
                            tempCanvas.drawRect(coords.getInt(0), coords.getInt(1), coords.getInt(2), coords.getInt(3), paint);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // DJDJ
                /*
                if (1 == 1) {
                    //Log.d("View", "jsonData : " + jsonData);
                    return;
                }
                */

                /*
                int scaledHeight = (imageView.getWidth()*myBitmap.getHeight())/myBitmap.getWidth();
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(newBitmap, imageView.getWidth(), scaledHeight, true);

                Log.d("View", "myBitmap : " + myBitmap.getWidth() + ", " + myBitmap.getHeight());
                Log.d("View", "imageView : " + imageView.getWidth() + ", " + imageView.getHeight());
                Log.d("View", "scaledBitmap : " + scaledBitmap.getWidth() + ", " + scaledBitmap.getHeight());
                */

                imageView.setImageBitmap(newBitmap);
                //imageView.setImageBitmap(scaledBitmap);
            }
        }
    }
}
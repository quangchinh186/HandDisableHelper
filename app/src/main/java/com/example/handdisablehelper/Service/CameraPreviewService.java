package com.example.handdisablehelper.Service;

import static org.opencv.android.NativeCameraView.TAG;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.handdisablehelper.R;
import com.example.handdisablehelper.Service.FloatingViewService;

import org.opencv.BuildConfig;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraPreviewService extends Service {
    public static CameraPreviewService instance = null;

    //bound int is the threshold to check for movement
    //counter will increase after user NOT moving the cursor and will perform click
    //at 100
    //is moving check for user movement
    int counter = 0, counterLimit = 50;
    int range = 30;
    boolean isMoving = false;
    String MODE = "tap";
    //View element
    View mFloatingView;
    WindowManager windowManager;
    int LAYOUT_FLAG;
    WindowManager.LayoutParams layoutParams;
    private TextureView textureView;

    //Android camera2 api
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private CascadeClassifier cascadeClassifier;
    private ImageReader imageReader;


    // kiểm tra trạng thái  ORIENTATION của ảnh đầu ra
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //thread for image processing
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    //cursor service
    FloatingViewService mService;
    boolean mBound = false;


    //FUNCTION
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Bound the cursor service to this service
     * **/
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            FloatingViewService.LocalBinder binder = (FloatingViewService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    
    @Override
    public void onCreate() {
        super.onCreate();
        startBackgroundThread();

        instance = this;

        Intent intent = new Intent(this, FloatingViewService.class);
        bindService(intent, connection, Context.BIND_IMPORTANT);
        startService(intent);

        OpenCVLoader.initDebug();
        initCascadeClassifier();

        SharedPreferences sharedPreferences = getSharedPreferences("app setting", MODE_PRIVATE);
        counterLimit = sharedPreferences.getInt("click timer", 50);
        range = sharedPreferences.getInt("range", 30);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        //camera preview layout
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.camera_preview, null);
        layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        layoutParams.x = 0;
        layoutParams.y = 100;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(mFloatingView, layoutParams);
        mFloatingView.setVisibility(View.VISIBLE);

        //texture view contain preview image
        textureView = mFloatingView.findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        return START_STICKY;
    }

    //Load the face-detection model
    void initCascadeClassifier(){
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
            FileOutputStream fileOutputStream = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int byteRead;
            while((byteRead = inputStream.read(buffer)) != -1){
                fileOutputStream.write(buffer, 0, byteRead);
            }
            inputStream.close();
            fileOutputStream.close();

            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    //face detect from openCV Mat
    Mat CascadeRec(Mat rgba){
        Core.flip(rgba.t(), rgba, 1);
        Mat rbg = new Mat();

        Imgproc.cvtColor(rgba, rbg, Imgproc.COLOR_RGBA2RGB);

        int height = rbg.height();
        int width = rbg.width();
        int center_input_y = height/2;
        int center_input_x = width/2;

        MatOfRect faces = new MatOfRect();
        if(cascadeClassifier != null){
            cascadeClassifier.detectMultiScale(rbg, faces, 1.1, 3, 0,
                    new org.opencv.core.Size(0, 0), new org.opencv.core.Size());
        }

        Rect[] facesArray = faces.toArray();
        for (Rect r: facesArray) {
            Imgproc.rectangle(rgba, r.tl(), r.br(), new Scalar(0, 255, 0, 255), 2);

        }
        if(facesArray.length != 0){
            int center_x, center_y;
            center_x = facesArray[0].x + (facesArray[0].width/2);
            center_y = facesArray[0].y + (facesArray[0].height/2);
            int leftBound = center_input_x + range,
                    rightBound = center_input_x - range,
                    topBound = center_input_y - range,
                    downBound = center_input_y + range;

            Log.v("face detected", "center at: " + center_x + "/" +center_y
                    + " while bound is" + leftBound + " " + rightBound
                    + " " + topBound + " " + downBound);

            if(center_x > leftBound){
                Log.v("faces", "left");
                isMoving = true;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(mService == null) return;
                        mService.moveCursor(mService.LEFT);
                    }
                });
            }
            if(center_x < rightBound){
                Log.v("faces", "right");
                isMoving = true;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(mService == null) return;
                        mService.moveCursor(mService.RIGHT);
                    }
                });
            }
            if(center_y < topBound){
                Log.v("faces", "up");
                isMoving = true;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(mService == null) return;
                        mService.moveCursor(mService.UP);
                    }
                });
            }
            if(center_y > downBound){
                Log.v("faces", "down");
                isMoving = true;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if(mService == null) return;
                        mService.moveCursor(mService.DOWN);
                    }
                });
            }

        }
        Core.flip(rgba.t(), rgba, 0);

        return rgba;
    }

    // Khởi tạo camera để preview trong textureview
    protected void createCameraPreview() {
        if(imageReader != null || cameraCaptureSessions != null) return;
        imageReader = ImageReader.newInstance(320, 320, ImageFormat.YUV_420_888, 1);
        imageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
        Log.v("camera", "create camera preview: Dimension " + imageDimension.toString());
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface(), surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //Toast.makeText(AndroidCameraApi.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // Open camera khi ready
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height, và thay đổi kích thước ảnh
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            // Camera opened
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = new Size(320, 320);
            //imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mFloatingView != null) windowManager.removeView(mFloatingView);
        stopBackgroundThread();
        closeCamera();
        instance = null;
        mService.onDestroy();
    }

    //frame processing
    private final ImageReader.OnImageAvailableListener onImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.v("image reader", "new image available");
                    Image image;
                    try {
                        isMoving = false;
                        image = reader.acquireLatestImage();
                        if(image == null) {
                            Log.d(TAG,"onImageAvailable: null image");
                            return;
                        }
                        //mat
                        Mat yuv= imageToMat(image);
                        CascadeRec(yuv);
                        Log.v("mat null?", yuv.toString());
                        if(!isMoving){
                            counter++;
                            if(counter % 10 == 0){
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(mService == null) return;
                                        mService.updateProgress(counter/counterLimit);
                                    }
                                });
                            }
                        } else {
                            counter = 0;
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if(mService == null) return;
                                    mService.updateProgress(0);
                                }
                            });
                        }


                        if(counter == counterLimit){
                            counter = 0;
                            int[] position = mService.getCursorPosition();
                            if(MODE.equals("tap")){
                                //dispatch click;
                                AppAccessibilityService.instance.performClick(position[0], position[1]);
                            } else {
                                AppAccessibilityService.instance.performScroll(position);
                            }
                        }

                    } catch (IllegalStateException e) {
                        Log.w(TAG, "Too many images queued, dropping image");
                        return;
                    }
                    image.close();
                }
            };

    //convert image to openCV Mat for processing
    static private Mat imageToMat(Image image) {
        ByteBuffer buffer;
        int rowStride;
        int pixelStride;
        int width = image.getWidth();
        int height = image.getHeight();
        int offset = 0;

        Log.v("image to mat","image size: " + width + " x " + height);

        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[image.getWidth() * image.getHeight() * 3 /2];// ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        for (int i = 0; i < planes.length; i++) {
            buffer = planes[i].getBuffer();
            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();
            int w = (i == 0) ? width : width / 2;
            int h = (i == 0) ? height : height / 2;
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
                if (pixelStride == bytesPerPixel) {
                    int length = w * bytesPerPixel;
                    buffer.get(data, offset, length);

                    // Advance buffer the remainder of the row stride, unless on the last row.
                    // Otherwise, this will throw an IllegalArgumentException because the buffer
                    // doesn't include the last padding.
                    if (h - row != 1) {
                        buffer.position(buffer.position() + rowStride - length);
                    }
                    offset += length;
                } else {

                    // On the last row only read the width of the image minus the pixel stride
                    // plus one. Otherwise, this will throw a BufferUnderflowException because the
                    // buffer doesn't include the last padding.
                    if (h - row == 1) {
                        buffer.get(rowData, 0, width - pixelStride + 1);
                    } else {
                        buffer.get(rowData, 0, rowStride);
                    }

                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
            }
        }

        // Finally, create the Mat.
        Mat mat = new Mat(height + height / 2, width, CvType.CV_8UC1);
        mat.put(0, 0, data);

        return mat;
    }

    public void setMode(String mode){
        MODE = mode;
    }

}

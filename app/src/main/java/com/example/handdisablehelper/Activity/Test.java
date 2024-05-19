package com.example.handdisablehelper.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.handdisablehelper.R;
import com.example.handdisablehelper.Service.AppAccessibilityService;
import com.example.handdisablehelper.Service.FloatingViewService;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Test extends CameraActivity {

    int counter = 0, counterLimit = 50;
    int range = 30;
    boolean isMoving = false;

    FloatingViewService mService;
    boolean mBound = false;
    /** Defines callbacks for service binding, passed to bindService(). */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            FloatingViewService.LocalBinder binder = (FloatingViewService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    CameraBridgeViewBase cameraBridgeViewBase;

    private CascadeClassifier cascadeClassifier;
    Button testBtn, backBtn;

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, FloatingViewService.class);
        bindService(intent, connection, Context.BIND_IMPORTANT);
        startService(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);

        testBtn = findViewById(R.id.testBtn);
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Random random = new Random();
                int color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
                testBtn.setBackgroundColor(color);
            }
        });
        backBtn = findViewById(R.id.done);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        cameraBridgeViewBase = findViewById(R.id.openCVCamera);
        cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        cameraBridgeViewBase.setMaxFrameSize(640, 640);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {}
            @Override
            public void onCameraViewStopped() {}
            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Mat rgba = inputFrame.rgba();

                Core.flip(rgba, rgba, 1);
                rgba = CascadeRec(rgba);

                if(isMoving){
                    counter++;
                    if(counter % 10 == 0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mService.updateProgress(counter);
                            }
                        });

                    }
                } else {
                    counter = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mService.updateProgress(0);
                        }
                    });

                }

                //perform click
                if(counter == counterLimit){
                    counter = 0;
                    int[] position = mService.getCursorPosition();
                    AppAccessibilityService.instance.performClick(position[0], position[1]);
                }

                return rgba;
            }
        });

        if(OpenCVLoader.initDebug()) {
            Log.v("OpenCV", "Success");
            cameraBridgeViewBase.enableView();
        }
        else Log.v("OpenCV", "err");

        initCascadeClassifier();
    }

    Mat CascadeRec(Mat rgba){
        isMoving = false;
        Log.v("cascade input in test",rgba.toString());
        //Imgcodecs.imwrite(Environment.getExternalStorageDirectory() + "/test.png", rgba.clone());

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
                    new Size(0,0), new Size());
        }

        Rect[] facesArray = faces.toArray();
        for (Rect r: facesArray) {
            Imgproc.rectangle(rgba, r.tl(), r.br(), new Scalar(0, 255, 0, 255), 2);
        }

        if(facesArray.length != 0){
            int center_x, center_y;
            center_x = facesArray[0].x + (facesArray[0].width/2);
            center_y = facesArray[0].y + (facesArray[0].height/2);
            Imgproc.drawMarker(rgba, new Point(center_x, center_y), new Scalar(255, 0, 0));
            int leftBound = center_input_x + range,
                    rightBound = center_input_x - range,
                    topBound = center_input_y - range,
                    downBound = center_input_y + range;

            Imgproc.rectangle(rgba, new Point(leftBound, topBound), new Point(rightBound, downBound),
                    new Scalar(0, 143, 255), 2);

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
                        mService.moveCursor(mService.RIGHT);
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
                        mService.moveCursor(mService.LEFT);
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

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mService.onDestroy();
    }
}
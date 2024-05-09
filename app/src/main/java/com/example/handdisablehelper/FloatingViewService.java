package com.example.handdisablehelper;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.ImageReader;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.util.Collections;
import java.util.List;

public class FloatingViewService extends Service {
    public final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3;
    //binding service
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        FloatingViewService getService() {
            // Return this instance of LocalService so clients can call public methods.
            return FloatingViewService.this;
        }
    }
    View mFloatingView;
    WindowManager windowManager;
    int LAYOUT_FLAG;

    WindowManager.LayoutParams layoutParams;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);

        layoutParams = new WindowManager.LayoutParams(200, 400,
                LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP|Gravity.RIGHT;
        layoutParams.x = 0;
        layoutParams.y = 100;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(mFloatingView, layoutParams);
        mFloatingView.setVisibility(View.VISIBLE);

        return START_STICKY;
    }

    public void moveCursor(int direction){
        switch (direction) {
            case LEFT:
                Log.v("move", "left");
                layoutParams.x = Math.max(layoutParams.x - 10, 0);
                break;

            case RIGHT:
                Log.v("move", "right");
                layoutParams.x = Math.min(layoutParams.x + 10, 2000);
                break;

            case UP:
                Log.v("move", "up");
                layoutParams.y = Math.max(layoutParams.y - 10, 0);
                break;

            case DOWN:
                Log.v("move", "down");
                layoutParams.y = Math.min(layoutParams.y + 10, 3000);
                break;

            default: break;

        }
        windowManager.updateViewLayout(mFloatingView, layoutParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mFloatingView != null) windowManager.removeView(mFloatingView);
    }
}

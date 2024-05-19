package com.example.handdisablehelper.Service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.example.handdisablehelper.R;

public class FloatingViewService extends Service {
    public static FloatingViewService instance = null;
    public final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3;
    private boolean isRunning;
    int SPEED = 10;
    //binding service
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public FloatingViewService getService() {
            // Return this instance of LocalService so clients can call public methods.
            return FloatingViewService.this;
        }
    }

    View mFloatingView;
    WindowManager windowManager;
    int LAYOUT_FLAG;
    WindowManager.LayoutParams layoutParams;
    int display_width, display_height;
    ProgressBar clickProgress;
    ImageView cursor;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    void getDisplaySize(){
        DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
        display_height = metrics.heightPixels;
        display_width = metrics.widthPixels;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        getDisplaySize();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        isRunning = true;
        //floating view
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);
        layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = 0;
        layoutParams.y = 0;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(mFloatingView, layoutParams);
        mFloatingView.setVisibility(View.VISIBLE);

        clickProgress = mFloatingView.findViewById(R.id.clickProgress);
        cursor = mFloatingView.findViewById(R.id.mCursor);

        return START_STICKY;
    }

    public void moveCursor(int direction){
        Log.v("cursor", layoutParams.x + "/" + display_width + " " + layoutParams.y + "/" + display_height);
        switch (direction) {
            case LEFT:
                Log.v("move", "left");
                layoutParams.x = layoutParams.x - SPEED;
                if(layoutParams.x < 0) layoutParams.x = 0;
                break;

            case RIGHT:
                Log.v("move", "right");
                layoutParams.x = layoutParams.x + SPEED;
                if(layoutParams.x > display_width) layoutParams.x = display_width-5;
                break;

            case UP:
                Log.v("move", "up");
                layoutParams.y = layoutParams.y - SPEED;
                if(layoutParams.y < 0) layoutParams.y = 0;
                break;

            case DOWN:
                Log.v("move", "down");
                layoutParams.y = layoutParams.y + SPEED;
                if(layoutParams.y > display_height) layoutParams.y = display_height-5;
                break;

            default: break;
        }
        windowManager.updateViewLayout(mFloatingView, layoutParams);
    }

    public int[] getCursorPosition(){
        int[] position = new int[2];
        position[0] = layoutParams.x;
        position[1] = layoutParams.y;

        return position;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if(mFloatingView != null) windowManager.removeView(mFloatingView);
    }

    public void updateProgress(int level){
        clickProgress.setProgress(level);
        windowManager.updateViewLayout(mFloatingView, layoutParams);
    }

    public void setMode(String mode){
        if(mode.equals("tap")){
            cursor.setImageResource(R.drawable.cursor);
        } else {
            cursor.setImageResource(R.drawable.scroll);
        }
    }

}

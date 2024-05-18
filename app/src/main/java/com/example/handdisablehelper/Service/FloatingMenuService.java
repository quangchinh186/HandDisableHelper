package com.example.handdisablehelper.Service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.SystemBarStyle;

import com.example.handdisablehelper.R;

public class FloatingMenuService extends Service {
    View menuView, menuContent;
    WindowManager windowManager;
    Button menuBtn;
    int LAYOUT_FLAG;
    WindowManager.LayoutParams layoutParams;
    public FloatingMenuService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        //camera preview layout
        menuView = LayoutInflater.from(this).inflate(R.layout.menu_button, null);
        layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        layoutParams.x = 0;
        layoutParams.y = 0;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(menuView, layoutParams);
        menuView.setVisibility(View.VISIBLE);

        menuContent = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        WindowManager.LayoutParams menuLayoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        menuLayoutParams.gravity = Gravity.CENTER;
        windowManager.addView(menuContent, menuLayoutParams);
        menuContent.setVisibility(View.GONE);

        startMenu();

        menuBtn = menuView.findViewById(R.id.menu);
        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("menu test", "open menu");
                menuContent.setVisibility(View.VISIBLE);
            }
        });
        
        return START_STICKY;
    }

    void startMenu(){
        Button vUp, vDown, tapMode, scrollMode, home, close;

        vUp = menuContent.findViewById(R.id.vUp);
        vUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
            }
        });

        vDown = menuContent.findViewById(R.id.vDown);
        vDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
            }
        });

        tapMode = menuContent.findViewById(R.id.tapMode);
        tapMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change to tap mode
                CameraPreviewService.instance.setMode("tap");
                FloatingViewService.instance.setMode("tap");
                menuContent.setVisibility(View.GONE);
            }
        });

        scrollMode = menuContent.findViewById(R.id.scrollMode);
        scrollMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change to scroll
                CameraPreviewService.instance.setMode("scroll");
                FloatingViewService.instance.setMode("scroll");
                menuContent.setVisibility(View.GONE);
            }
        });

        home = menuContent.findViewById(R.id.home);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        close = menuContent.findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuContent.setVisibility(View.GONE);
            }
        });

    }
}
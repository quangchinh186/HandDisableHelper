package com.example.handdisablehelper.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.handdisablehelper.R;
import com.example.handdisablehelper.Service.CameraPreviewService;
import com.example.handdisablehelper.Service.FloatingMenuService;

import org.opencv.android.OpenCVLoader;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    /*
    * This is the main activity that will appear when user first start
    * The purpose of this activity is to check permission and give some options
    * */

    Button start, test, setting;
    boolean hasAccessibilityPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getCameraPermission();
        getOverlayPermission();

        hasAccessibilityPermission = checkAccessibilityPermission();
        start = findViewById(R.id.startService);
        test = findViewById(R.id.test);
        setting = findViewById(R.id.setting);

        OpenCVLoader.initDebug();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasAccessibilityPermission){
                    Toast.makeText(getApplicationContext(), "Please enable app's Accessibility in device setting", Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(getApplicationContext(), CameraPreviewService.class);
                    Intent menu = new Intent(getApplicationContext(), FloatingMenuService.class);
                    startService(intent);
                    startService(menu);
                    finish();
                }
            }
        });

        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasAccessibilityPermission){
                    Toast.makeText(getApplicationContext(), "Please enable app's Accessibility in device setting", Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(getApplicationContext(), TestAndSetting.class);
                    startActivity(intent);
                }
            }
        });

        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Tutorial2.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        hasAccessibilityPermission = checkAccessibilityPermission();
    }

    boolean checkAccessibilityPermission(){
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for(AccessibilityServiceInfo asi : enabled){
            ServiceInfo serviceInfo = asi.getResolveInfo().serviceInfo;
            if(serviceInfo.packageName.equals(getPackageName())) return true;
        }

        return false;
    }
    void getCameraPermission(){
        if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }
    }
    void getOverlayPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)){
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
            startActivityForResult(intent, 103);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
            getCameraPermission();
        }
        if(requestCode == 103){
            if(!Settings.canDrawOverlays(MainActivity.this)){
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
            }
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
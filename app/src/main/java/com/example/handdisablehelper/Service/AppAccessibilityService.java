package com.example.handdisablehelper.Service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AppAccessibilityService extends AccessibilityService {
    public static AppAccessibilityService instance;
    String TAG = "AppAccessibilityService";
    int display_width, display_height;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        Log.v(TAG, "new event");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        //Toast.makeText(this, "accessibility service start", Toast.LENGTH_LONG).show();
        instance = this;
        getDisplaySize();
        Log.v(TAG, "new A service connected");
    }

    public void test(){
        Log.v(TAG, "test called");
    }

    public void performClick(int x, int y){
        Log.v("clicking", "perform click via accessibility service at: " + x + " " + y);
        final int DURATION = 5;

        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, DURATION);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);

        dispatchGesture(clickBuilder.build(), null, null);
    }

    public void performScroll(int[] end){

        Log.v(TAG, "perform scroll via accessibility service from: "
                + end[0] + "/" + display_height/2 + " to: " + end[0] + "/" + end[1]);

        Path swipePath = new Path();

        //swipe up
        if(end[1] < display_height/2){
            swipePath.moveTo(end[0], end[1]);
            swipePath.lineTo(end[0], (display_height/2) + 100);
        } else {
            //swipe down
            swipePath.moveTo(end[0], end[1]);
            swipePath.lineTo(end[0], (display_height/2) - 100);
        }


        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 500));

        dispatchGesture(gestureBuilder.build(), null, null);
    }

    void getDisplaySize(){
        DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
        display_height = metrics.heightPixels;
        display_width = metrics.widthPixels;
    }

    @Override
    public void onInterrupt() {

    }
}

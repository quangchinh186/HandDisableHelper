package com.example.handdisablehelper.Service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AppAccessibilityService extends AccessibilityService {
    public static AppAccessibilityService instance;
    String TAG = "AppAccessibilityService";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        Log.v(TAG, "new event");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        //Toast.makeText(this, "accessibility service start", Toast.LENGTH_LONG).show();
        instance = this;
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

    public void performScroll(int[] start, int[] end){
        Log.v(TAG, "perform scroll via accessibility service from: "
                + start[0] + "/" + start[1] + " to: " + end[0] + "/" + end[1]);

        Path swipePath = new Path();
        swipePath.moveTo(start[0], start[1]);
        swipePath.lineTo(end[0], end[1]);
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 500));

        dispatchGesture(gestureBuilder.build(), null, null);
    }

    @Override
    public void onInterrupt() {

    }
}

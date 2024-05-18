package com.example.handdisablehelper.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.handdisablehelper.R;
import com.google.android.material.slider.Slider;

public class Setting extends AppCompatActivity {

    Slider cursorSpeed, clickTimer, detectRange;
    TextView speedText, timerText, rangeText;
    Button saveBtn, dismissBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("app setting", MODE_PRIVATE);


        cursorSpeed = findViewById(R.id.speedSlider);
        clickTimer = findViewById(R.id.timerSlider);
        detectRange = findViewById(R.id.rangeSilder);
        speedText = findViewById(R.id.text1);
        timerText = findViewById(R.id.text2);
        rangeText = findViewById(R.id.text3);
        saveBtn = findViewById(R.id.saveBtn);
        dismissBtn = findViewById(R.id.dismissBtn);



        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
    }
}
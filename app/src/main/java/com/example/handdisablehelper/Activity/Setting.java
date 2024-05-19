package com.example.handdisablehelper.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.handdisablehelper.R;
import com.google.android.material.slider.Slider;

public class Setting extends AppCompatActivity {
    int speed, timer, range;

    Slider cursorSpeed, clickTimer, detectRange;
    TextView speedText, timerText, rangeText;
    Button saveBtn, dismissBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        SharedPreferences sharedPreferences = getSharedPreferences("app setting", MODE_PRIVATE);
        cursorSpeed = findViewById(R.id.speedSlider);
        clickTimer = findViewById(R.id.timerSlider);
        detectRange = findViewById(R.id.rangeSlider);
        speedText = findViewById(R.id.text1);
        timerText = findViewById(R.id.text2);
        rangeText = findViewById(R.id.text3);
        saveBtn = findViewById(R.id.saveBtn);
        dismissBtn = findViewById(R.id.dismissBtn);

        speed = sharedPreferences.getInt("speed", 10);
        timer = sharedPreferences.getInt("timer", 50);
        range = sharedPreferences.getInt("range", 30);

        cursorSpeed.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                speedText.setText("Cursor speed: " + (int)value);
                speed = (int) value;
            }
        });

        clickTimer.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                timerText.setText("Click timer: " + (int)value);
                timer = (int) value;
            }
        });

        detectRange.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                rangeText.setText("Detect range: " + (int)value);
                range = (int)value;
            }
        });

        cursorSpeed.setValue(speed);
        clickTimer.setValue(timer);
        detectRange.setValue(range);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreferences.edit().putInt("speed", speed).apply();
                sharedPreferences.edit().putInt("range", range).apply();
                sharedPreferences.edit().putInt("timer", timer).apply();
                finish();
            }
        });

        dismissBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}
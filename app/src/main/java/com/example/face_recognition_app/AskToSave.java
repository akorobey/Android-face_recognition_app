package com.example.face_recognition_app;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

public class AskToSave extends AppCompatActivity {
        ImageView newFaceImage;
        Button saveButton;
        Button cancelButton;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_ask_to_save);

            newFaceImage = findViewById(R.id.imageView);
            saveButton = findViewById(R.id.save_button);
            cancelButton = findViewById(R.id.cancel_button);
        }

}
package com.example.face_recognition_app;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class AskToSave extends AppCompatActivity {
        ImageView newFaceImage;
        Button saveButton;
        Button cancelButton;

        EditText inputName;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_ask_to_save);

            newFaceImage = findViewById(R.id.imageView);
            saveButton = findViewById(R.id.save_button);
            cancelButton = findViewById(R.id.cancel_button);
            inputName = findViewById(R.id.input_name);

            // get Bitmap from  Extras
            byte[] byteArr = getIntent().getExtras().getByteArray("new_face");
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length);

            newFaceImage.setImageBitmap(bitmap);
        }

    public void cancel(View view) {
        onBackPressed();
    }

    public void save(View view) {
        FileOutputStream fos = null;
        try {
            String file_name = inputName.getText().toString();
            file_name += ".png";

            fos = openFileOutput(file_name, MODE_PRIVATE);
            Bitmap pictureBitmap = ((BitmapDrawable) newFaceImage.getDrawable()).getBitmap();
            pictureBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Toast.makeText(this, "Файл сохранен", Toast.LENGTH_SHORT).show();
        }
        catch(IOException ex) {

            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        } finally{
            try{
                if(fos!=null)
                    fos.close();
            }
            catch(IOException ex){

                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        onBackPressed();
    }
}

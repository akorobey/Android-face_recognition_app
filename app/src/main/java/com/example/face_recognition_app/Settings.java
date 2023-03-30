package com.example.face_recognition_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

public class Settings extends AppCompatActivity {

    RecyclerView settings;

    String [] allSettings = {"Управление галереей", "Выбор устройства"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = (RecyclerView) findViewById(R.id.settings_list);

        // create adapter for all settings
        RecyclerView.Adapter<SettingAdapter.ViewHolder> adapter = new SettingAdapter(allSettings, this);

        settings.setAdapter(adapter);
    }
}

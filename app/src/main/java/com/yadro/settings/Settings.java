package com.yadro.settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.yadro.face_recognition_app.R;

public class Settings extends AppCompatActivity {

    RecyclerView settings;

    String [] allSettings = {"Управление галереей", "Управление мониторами", "Выбор устройства", "Число потоков (для CPU)",
                             "Выбор алгоритма"};

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

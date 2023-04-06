package com.yadro.settings;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.yadro.face_recognition_app.R;

public class Monitors extends AppCompatActivity {

    String[] monitors = {"c (CPU monitor)", "d (Distribution CPU)", "m (Memory)"};
    TextView selection;
    ListView monitorsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenters);

        selection = findViewById(R.id.selection);
        monitorsList = findViewById(R.id.monitorsList);
        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_multiple_choice, monitors);

        monitorsList.setAdapter(adapter);
        SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE);
        initView(settings);

        monitorsList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                SparseBooleanArray selected = monitorsList.getCheckedItemPositions();
                SharedPreferences.Editor prefEditor = settings.edit();

                String selectedItems = "";
                String current = "";
                for(int i=0; i < monitors.length; i++)
                {
                    if(selected.get(i)) {
                        String sym = monitors[i].split(" ")[0];
                        selectedItems += sym;
                        current += sym;
                    }
                }
                prefEditor.putString("Monitors", current);
                prefEditor.apply();
                selection.setText("Текущее: " + selectedItems);
            }
        });
    }

    private void initView(SharedPreferences settings) {
        String currentPresenters = settings.getString("Monitors", "");
        String current = "";
        if (currentPresenters.contains("c")) {
            current += "c";
            monitorsList.setItemChecked(0, true);
        }
        if (currentPresenters.contains("d")) {
            current += "d";
            monitorsList.setItemChecked(1, true);
        }
        if (currentPresenters.contains("m")) {
            current += "m";
            monitorsList.setItemChecked(2, true);
        }

        selection.setText("Текущее: " + current);
    }
}
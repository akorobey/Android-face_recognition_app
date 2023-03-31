package com.yadro.face_recognition_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

public class FacesList extends AppCompatActivity {

    RecyclerView listFaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faces_list);

        listFaces = (RecyclerView) findViewById(R.id.faces_list);

        // create adapter for all settings
        RecyclerView.Adapter<FileAdapter.ViewHolder> adapter = new FileAdapter(getFilesDir(), fileList(), this);

        listFaces.setAdapter(adapter);

    }
}
package com.yadro.face_recognition_app;

import java.util.ArrayList;

public class GalleryObject {
    ArrayList<Float> embeddings;
    String label;
    int id;

    public GalleryObject(ArrayList<Float> embeddings_, String label_, int id_) {
        embeddings = embeddings_;
        label = label_;
        id = id_;
    }
}

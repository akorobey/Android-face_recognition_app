package com.yadro.gallery;

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

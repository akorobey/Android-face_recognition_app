package com.yadro.tfmodels;

import android.graphics.Rect;

public class Face {
    public Rect face;
    public String label;

    public Face (Rect box, String name) {
        this.face = box;
        this.label = name;
    }
}

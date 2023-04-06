package com.yadro.face_recognition_app;

import android.graphics.Bitmap;
import androidx.camera.core.ImageProxy;

import com.yadro.graphics.GraphicOverlay;

/** An interface to process the images with different vision detectors and custom image models. */
public interface VisionImageProcessor {
    /** Processes a bitmap image. */
    void processBitmap(Bitmap bitmap, GraphicOverlay graphicOverlay);

    /** Processes ImageProxy image data, e.g. used for CameraX live preview case. */
    void processImageProxy(Bitmap bitmap, GraphicOverlay graphicOverlay);

    /** Stops the underlying machine learning model and release resources. */
    void stop();
}

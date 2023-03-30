package com.yadro.face_recognition_app;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

/** Draw camera image to background. */
public class CameraImageGraphic extends GraphicOverlay.Graphic {

    private final Bitmap bitmap;
    private final Presenter presenter;

    public CameraImageGraphic(GraphicOverlay overlay, Bitmap bitmap, Presenter presenter) {
        super(overlay);
        this.bitmap = bitmap;
        this.presenter = presenter;
    }

    @Override
    public void draw(Canvas canvas) {
        Mat frame = new Mat();
        Utils.bitmapToMat(bitmap, frame);
        presenter.drawGraphs(frame);
        Utils.matToBitmap(frame, bitmap);
        canvas.drawBitmap(bitmap, getTransformationMatrix(), null);
    }
}
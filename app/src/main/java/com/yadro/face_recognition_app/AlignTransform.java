package com.yadro.face_recognition_app;


import android.graphics.PointF;
import android.graphics.Rect;

public class AlignTransform {
    static final private float roiEnlargeCoeff = 1.0f;

    public static final Rect enlargeFaceRoi(Rect roi, int width, int height) {
        Rect enlargedRoi = new Rect();
        int inflationX = (Math.round(roi.width() * roiEnlargeCoeff) - roi.width()) / 2;
        int inflationY = (Math.round(roi.height() * roiEnlargeCoeff) - roi.height()) / 2;
        enlargedRoi.left = (roi.left - inflationX) < 0 ? 0 : roi.left - inflationX;
        enlargedRoi.top =  (roi.top - inflationY) < 0 ? 0 : roi.top - inflationY;
        enlargedRoi.right = (roi.right + inflationX) > width ? width : roi.right + inflationX;
        enlargedRoi.bottom = (roi.bottom + inflationY) > height ? height : roi.bottom + inflationY;
        return enlargedRoi;
    }

    public static final double calculateRotationRad(PointF p0, PointF p1) {
        double rad = -Math.atan2(p0.y - p1.y, p1.x - p0.x);
        double radNormed = rad - 2 * Math.PI * Math.floor((rad + Math.PI) / (2 * Math.PI));  // normalized to [0, 2*PI]
        return radNormed;
    }

    public static final float[] rotatePoints(float[] points,
                                             double rad,
                                             final PointF rotCenter) {
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        float[] res = new float[points.length];
        for (int i = 0; i < points.length / 2; ++i) {
            float x = points[2 * i];
            float y = points[2 * i + 1];
            x -= rotCenter.x;
            y -= rotCenter.y;
            float newX = (float) (x * cos - y * sin);
            float newY = (float) (x * sin + y * cos);
            newX += rotCenter.x;
            newY += rotCenter.y;

            res[2 * i] = newX;
            res[2 * i + 1] = newY;
        }
        return res;
    }
}

package com.yadro.utils;


import android.graphics.PointF;
import android.graphics.Rect;

public class AlignTransform {

    public static Rect enlargeFaceRoi(Rect roi, int width, int height, float roiEnlargeCoeff) {
        Rect enlargedRoi = new Rect();
        int inflationX = (Math.round(roi.width() * roiEnlargeCoeff) - roi.width()) / 2;
        int inflationY = (Math.round(roi.height() * roiEnlargeCoeff) - roi.height()) / 2;
        enlargedRoi.left = Math.max((roi.left - inflationX), 0);
        enlargedRoi.top = Math.max((roi.top - inflationY), 0);
        enlargedRoi.right = Math.min((roi.right + inflationX), width);
        enlargedRoi.bottom = Math.min((roi.bottom + inflationY), height);
        return enlargedRoi;
    }

    public static double calculateRotationRad(PointF p0, PointF p1) {
        double rad = -Math.atan2(p0.y - p1.y, p1.x - p0.x);
        return rad - 2 * Math.PI * Math.floor((rad + Math.PI) / (2 * Math.PI));
    }

    public static float[] rotatePoints(float[] points,
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

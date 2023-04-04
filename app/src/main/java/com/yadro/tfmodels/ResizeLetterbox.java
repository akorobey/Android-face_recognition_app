package com.yadro.tfmodels;

import static java.lang.Math.min;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.tensorflow.lite.support.common.internal.SupportPreconditions;
import org.tensorflow.lite.support.image.ColorSpaceType;
import org.tensorflow.lite.support.image.ImageOperator;
import org.tensorflow.lite.support.image.TensorImage;

public class ResizeLetterbox implements ImageOperator {
    private final int targetHeight;
    private final int targetWidth;

    private float scale;
    private int xPadding;
    private int yPadding;
    private final Bitmap output;

    public ResizeLetterbox(int targetHeight, int targetWidth) {
        this.targetHeight = targetHeight;
        this.targetWidth = targetWidth;
        this.output = Bitmap.createBitmap(this.targetWidth, this.targetHeight, Config.ARGB_8888);
    }

    public @NonNull TensorImage apply(@NonNull TensorImage image) {
        SupportPreconditions.checkArgument(image.getColorSpaceType() == ColorSpaceType.RGB, "Only RGB images are supported in ResizeWithCropOrPadOp, but not " + image.getColorSpaceType().name());
        Bitmap input = image.getBitmap();
        int w = input.getWidth();
        int h = input.getHeight();
        scale = min((float) targetWidth / w, (float) targetHeight / h);
        Bitmap scaled = Bitmap.createScaledBitmap(image.getBitmap(), Math.round(w * scale), Math.round(h * scale), true);
        xPadding = (targetWidth - scaled.getWidth()) / 2;
        yPadding = (targetHeight - scaled.getHeight()) / 2;
        Rect src = new Rect(0, 0,  scaled.getWidth(), scaled.getHeight());
        Rect dst = new Rect(xPadding, yPadding, scaled.getWidth() + xPadding, scaled.getHeight() + yPadding);
        (new Canvas(this.output)).drawBitmap(scaled, src, dst, (Paint)null);
        image.load(this.output);
        return image;
    }

    public int getOutputImageHeight(int inputImageHeight, int inputImageWidth) {
        return this.targetHeight;
    }

    public int getOutputImageWidth(int inputImageHeight, int inputImageWidth) {
        return this.targetWidth;
    }

    public PointF inverseTransform(PointF point, int inputImageHeight, int inputImageWidth) {
        return transformImpl(point, xPadding, yPadding, scale);
    }

    private static PointF transformImpl(PointF point, int xPadding, int yPadding, float scale) {
        return new PointF((point.x - xPadding) / scale, (point.y - yPadding) / scale);
    }
}

package com.yadro.utils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build.VERSION_CODES;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/** Utils functions for bitmap conversions. */
public class BitmapUtils {
    private static final String TAG = "BitmapUtils";

    public static Bitmap getBitmap(String file) {
        return BitmapFactory.decodeFile(file);
    }

    /** Converts a YUV_420_888 image from CameraX API to a bitmap. */
    @RequiresApi(VERSION_CODES.LOLLIPOP)
    @Nullable
    @ExperimentalGetImage
    public static Bitmap getBitmap(ImageProxy imageProxy, boolean isImageFlipped) {
        Image image = imageProxy.getImage();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        return rotateBitmap(bmp, imageProxy.getImageInfo().getRotationDegrees(), isImageFlipped);
    }

    /** Converts a RGBA_8888 image from CameraX API to a bitmap. */
    @ExperimentalGetImage
    public static Bitmap getBitmapRGBA_8888(ImageProxy imageProxy, boolean isImageFlipped) {
        Image image = imageProxy.getImage();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        Bitmap bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding/pixelStride,
                image.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return rotateBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees(), isImageFlipped);
    }

    /** Rotates a bitmap if it is converted from a bytebuffer. */
    public static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees, boolean isImageFlipped) {
        Matrix matrix = new Matrix();

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees);

        // Mirror the image along the X or Y axis.
        if (isImageFlipped) {
            // to keep image upright on camera rotation
            matrix.postScale(-1, 1);
        } else {
            matrix.postScale(1, 1);
        }
        Bitmap rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }
}

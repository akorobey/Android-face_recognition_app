package com.yadro.face_recognition_app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class FaceDetectorProcessor extends VisionProcessorBase<List<Face>> {
    private static final String TAG = "FaceDetectorProcessor";
    private final FaceDetector detector;
    FaceRecognitionModel recognizer;
    FaceGallery gallery;

    private Context mainApp;

    public FaceDetectorProcessor(Context context) throws IOException, URISyntaxException {
        super(context);
        this.mainApp = context;
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();
        detector = FaceDetection.getClient(options);

        // Init Face Recognition model
        String modelFile = null;
        try {
            modelFile = Common.getResourcePath(context.getAssets().open("mobilefacenet.tflite"), "mobilefacenet", "tflite");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        recognizer = new FaceRecognitionModel(modelFile, 4);
        gallery = new FaceGallery(context, recognizer);
    }
    public Task<List<Face>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    public void showDialog(Bitmap faceImage) {
        AddFaceDialogFragment dialog = new AddFaceDialogFragment();
        Bundle args = new Bundle();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        faceImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        faceImage.recycle();
        args.putByteArray("face_image", byteArray);
        dialog.setArguments(args);
        Activity main = (Activity) mainApp;
        dialog.show(main.getFragmentManager(), "dialog");
    }

    public void startAskToSaveActivity(Bitmap faceImage) {
        // get ByteArray from Bitmap
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        faceImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Intent newWindow = new Intent(mainApp, AskToSave.class);
        newWindow.putExtra("new_face", byteArray);
        mainApp.startActivity(newWindow);
    }

    @Override
    protected void onSuccess(@NonNull Bitmap originalCameraImage, @NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay) {
        ArrayList<ArrayList<Float>> allEmb = new ArrayList<ArrayList<Float>>();
        for (Face face : faces) {
            Bitmap rotatedFace = getFaceFromImage(originalCameraImage, face);
            ArrayList<Float> embeddings = recognizer.run(rotatedFace);
            allEmb.add(embeddings);
        }

        ArrayList<Pair<Integer, Float>> matches = gallery.getIDsByEmbeddings(allEmb);
        MainActivity mainApp1 = (MainActivity) mainApp;

        for (int i = 0; i < matches.size(); ++i) {
            if (matches.get(i).first == gallery.unknownId) {
                if (mainApp1.allowGrow || mainApp1.editMode.isPressed()) {
                    startAskToSaveActivity(getFaceFromImage(originalCameraImage, faces.get(i)));
                }
            }
        }
        if (mainApp1.allowGrow) {
            mainApp1.editMode.performClick();
        }


        for (int i = 0; i < faces.size(); ++i) {
            graphicOverlay.add(new FaceGraphic(graphicOverlay, faces.get(i), gallery.getLabelByID(matches.get(i).first)));
        }
    }

    protected  Bitmap getFaceFromImage(Bitmap source, Face face) {
        int inputWidth = source.getWidth();
        int inputHeight = source.getHeight();
        Rect faceRect = AlignTransform.enlargeFaceRoi(face.getBoundingBox(), inputWidth, inputHeight);
        int faceRoiWidth = faceRect.width();
        int faceRoiHeight = faceRect.height();
        PointF rotationCenter = new PointF((faceRect.left + faceRect.right) * 0.5f, (faceRect.top + faceRect.bottom) * 0.5f);

        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        double rotationRad = AlignTransform.calculateRotationRad(leftEye.getPosition(), rightEye.getPosition());

        float[] dstPoints = {0, 0,
                inputWidth, 0,
                inputWidth, inputHeight,
                0, inputHeight};

        float[] srcPoints = {faceRect.left, faceRect.top,
                faceRect.right, faceRect.top,
                faceRect.right, faceRect.bottom,
                faceRect.left, faceRect.bottom};

        srcPoints = AlignTransform.rotatePoints(srcPoints, rotationRad, rotationCenter);

        Matrix m = new Matrix();
        m.setPolyToPoly(srcPoints, 0, dstPoints, 0, dstPoints.length >> 1);
        Bitmap dstBitmap = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dstBitmap);
        canvas.clipRect(0, 0, inputWidth, inputHeight);
        canvas.drawBitmap(source, m, null);

        return  dstBitmap;
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }
}
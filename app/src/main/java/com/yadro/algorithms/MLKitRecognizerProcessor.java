package com.yadro.algorithms;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.yadro.face_recognition_app.VisionImageProcessor;
import com.yadro.gallery.AskToSave;
import com.yadro.gallery.FaceGallery;
import com.yadro.graphics.MLKitFaceGraphic;
import com.yadro.graphics.GraphicOverlay;
import com.yadro.tfmodels.FaceRecognitionModel;
import com.yadro.utils.AlignTransform;
import com.yadro.utils.Common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MLKitRecognizerProcessor implements VisionImageProcessor {
    private static final String TAG = "FaceDetectorProcessor";
    private final FaceDetector detector;
    FaceRecognitionModel recognizer;
    float scale = 1.0f;
    FaceGallery gallery;

    private Context mainApp;

    public MLKitRecognizerProcessor(Context context) throws IOException, URISyntaxException {
        this.mainApp = context;
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setMinFaceSize(0.2f)
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
        String device = mainApp.getSharedPreferences("Settings", MODE_PRIVATE).getString("Device", "CPU");
        int threads = mainApp.getSharedPreferences("Settings", MODE_PRIVATE).getInt("Threads", 1);

        recognizer = new FaceRecognitionModel(modelFile, device, threads);
        gallery = new FaceGallery(context, recognizer);
    }
    public Task<List<Face>> detectInImage(Bitmap image) {
        // Some resize before provide to detector
        //Bitmap resized = Bitmap.createScaledBitmap(image, (int) (image.getWidth() / scale), (int) (image.getHeight() / scale), true);

        System.out.println("Size of image before DETECTOR = " + image.getWidth() + "x" + image.getHeight());
        return detector.process(InputImage.fromBitmap(image, 0));
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

    protected void onSuccess(@NonNull Bitmap originalCameraImage, @NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay) {
        // TODO postprocess with scale to source image
        ArrayList<ArrayList<Float>> allEmb = new ArrayList<ArrayList<Float>>();
        for (Face face : faces) {
            Bitmap rotatedFace = getFaceFromImage(originalCameraImage, face, recognizer.inputWidth, recognizer.inputHeight);
            ArrayList<Float> embeddings = recognizer.run(rotatedFace);
            allEmb.add(embeddings);
        }

        ArrayList<Pair<Integer, Float>> matches = gallery.getIDsByEmbeddings(allEmb);

        boolean allow_grow = mainApp.getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("AllowGrow", false);
        for (int i = 0; i < matches.size(); ++i) {
            if (matches.get(i).first == gallery.unknownId) {
                if (allow_grow) {
                    startAskToSaveActivity(getFaceFromImage(originalCameraImage, faces.get(i), 300, 300));
                }
            }
        }
        if (allow_grow) {
            SharedPreferences settings = mainApp.getSharedPreferences("Settings", MODE_PRIVATE);
            SharedPreferences.Editor prefEditor = settings.edit();
            prefEditor.putBoolean("AllowGrow", false);
            prefEditor.apply();
        }


        for (int i = 0; i < faces.size(); ++i) {
            graphicOverlay.add(new MLKitFaceGraphic(graphicOverlay, faces.get(i), gallery.getLabelByID(matches.get(i).first)));
        }
    }

    protected  Bitmap getFaceFromImage(Bitmap source, Face face, int inputWidth, int inputHeight) {
        int imageWidth = source.getWidth();
        int imageHeight = source.getHeight();
        Rect faceRect = AlignTransform.enlargeFaceRoi(face.getBoundingBox(), imageWidth, imageHeight, 1.0f);
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
    public void processBitmap(Bitmap bitmap, GraphicOverlay graphicOverlay) {

    }

    @Override
    public void processImageProxy(Bitmap bitmap, GraphicOverlay graphicOverlay) {
        Task<List<Face>> task = detectInImage(bitmap);
        while (!task.isComplete());
        onSuccess(bitmap, task.getResult(), graphicOverlay);
    }

    @Override
    public void stop() {
    }
}

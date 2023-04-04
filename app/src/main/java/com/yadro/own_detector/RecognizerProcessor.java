package com.yadro.own_detector;

import static android.content.Context.MODE_PRIVATE;
import static com.yadro.utils.Common.getResourcePath;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.yadro.face_recognition_app.MainActivity;
import com.yadro.face_recognition_app.VisionImageProcessor;
import com.yadro.gallery.AskToSave;
import com.yadro.gallery.FaceGallery;
import com.yadro.graphics.CameraImageGraphic;
import com.yadro.graphics.FaceGraphic;
import com.yadro.graphics.InferenceInfoGraphic;
import com.yadro.graphics.MLKitFaceGraphic;
import com.yadro.graphics.GraphicOverlay;
import com.yadro.mlkit_detector.VisionProcessorBase;
import com.yadro.tfmodels.BBox;
import com.yadro.tfmodels.Face;
import com.yadro.tfmodels.BlazeFace;
import com.yadro.tfmodels.FaceRecognitionModel;
import com.yadro.utils.AlignTransform;
import com.yadro.utils.BitmapUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class RecognizerProcessor implements VisionImageProcessor {
    private static final String TAG = "FaceRecognitionProcessor";
    private BlazeFace detector;
    private FaceRecognitionModel recognizer;

    // For FPS
    private final Timer fpsTimer = new Timer();
    float scale = 1.0f;
    FaceGallery gallery;

    private Context mainApp;

    public RecognizerProcessor(Context context) throws IOException, URISyntaxException {
        this.mainApp = context;
        // Init Face Detection and Recognition models
        initModels();

        // Init FaceGallery
        gallery = new FaceGallery(context, recognizer);
    }
    private void initModels() {
        String modelFile = "";
        try {
            modelFile = getResourcePath(mainApp.getAssets().open("face_detection_short_range.tflite"), "face_detection_short_range", "tflite");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        String device = mainApp.getSharedPreferences("Settings", MODE_PRIVATE).getString("Device", "CPU");
        int threads = mainApp.getSharedPreferences("Settings", MODE_PRIVATE).getInt("Threads", 1);
        detector = new BlazeFace(modelFile, device, threads);

        try {
            modelFile = getResourcePath(mainApp.getAssets().open("mobilefacenet.tflite"), "mobilefacenet", "tflite");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        recognizer = new FaceRecognitionModel(modelFile, device, threads);
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

    protected  Bitmap getFaceFromImage(Bitmap source, BBox box) {
        int inputWidth = source.getWidth();
        int inputHeight = source.getHeight();
        Rect faceRect = AlignTransform.enlargeFaceRoi(box.face, inputWidth, inputHeight, 1.2f);
        PointF rotationCenter = new PointF((faceRect.left + faceRect.right) * 0.5f, (faceRect.top + faceRect.bottom) * 0.5f);

        PointF leftEye = box.leftEye;
        PointF rightEye = box.rightEye;
        double rotationRad = AlignTransform.calculateRotationRad(leftEye, rightEye);

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
    @ExperimentalGetImage
    public void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) {
        long startFrameTime = SystemClock.elapsedRealtime();
        Bitmap bitmap = BitmapUtils.getBitmap(image);
        // Detect faces
        assert bitmap != null;
        long startDetectorTime = SystemClock.elapsedRealtime();
        ArrayList<BBox> boxes = detector.run(bitmap);

        // Recognize faces
        ArrayList<ArrayList<Float>> allEmb = new ArrayList<ArrayList<Float>>();
        for (BBox box : boxes) {
            Bitmap rotatedFace = getFaceFromImage(bitmap, box);
            ArrayList<Float> embeddings = recognizer.run(rotatedFace);
            allEmb.add(embeddings);
        }

        ArrayList<Pair<Integer, Float>> matches = gallery.getIDsByEmbeddings(allEmb);

        boolean allow_grow = mainApp.getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("AllowGrow", false);

        for (int i = 0; i < matches.size(); ++i) {
            if (matches.get(i).first == gallery.unknownId) {
                if (allow_grow) {
                    startAskToSaveActivity(getFaceFromImage(bitmap, boxes.get(i)));
                }
            }
        }
        if (allow_grow) {
            SharedPreferences settings = mainApp.getSharedPreferences("Settings", MODE_PRIVATE);
            SharedPreferences.Editor prefEditor = settings.edit();
            prefEditor.putBoolean("AllowGrow", false);
            prefEditor.apply();
        }
        long endDetectorTime = SystemClock.elapsedRealtime();
        long currentDetectorLatencyMs = endDetectorTime - startDetectorTime;

        // TODO render faces
        graphicOverlay.clear();
        if (bitmap != null) {
            graphicOverlay.add(new CameraImageGraphic(graphicOverlay, bitmap, null));
        }


        for (int i = 0; i < boxes.size(); ++i) {
            graphicOverlay.add(new FaceGraphic(graphicOverlay,
                               new Face(AlignTransform.enlargeFaceRoi(boxes.get(i).face, bitmap.getWidth(),
                                       bitmap.getHeight(), 1.2f), gallery.getLabelByID(matches.get(i).first))));
        }

        long endFrameTime = SystemClock.elapsedRealtime();
        long currentFrameLatencyMs = endFrameTime - startFrameTime;
        int fps = (int) (1000.f / (currentFrameLatencyMs));
        graphicOverlay.add(
                new InferenceInfoGraphic(
                        graphicOverlay,
                        currentFrameLatencyMs,
                        currentDetectorLatencyMs,
                        fps));
        graphicOverlay.postInvalidate();
        image.close();
    }

    @Override
    public void stop() {
        fpsTimer.cancel();
    }
}

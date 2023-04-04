package com.yadro.face_recognition_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.mlkit.common.MlKitException;
import com.yadro.graphics.GraphicOverlay;
import com.yadro.mlkit_detector.FaceDetectorProcessor;
import com.yadro.own_detector.RecognizerProcessor;
import com.yadro.settings.Settings;

import java.io.IOException;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "FaceRecognition demo";

    // Permissions
    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS =
            new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    // View elements
    public ImageButton editMode;
    ImageButton switchCamera;
    ImageButton settingsButton;
    public boolean allowGrow = false;

    // CameraX usecases
    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    @Nullable private ProcessCameraProvider cameraProvider;
    @Nullable private Preview previewUseCase;
    @Nullable private ImageAnalysis analysisUseCase;
    @Nullable private VisionImageProcessor imageProcessor;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private final String LENS_FACING_KEY = "LENS_FACING";
    private boolean needUpdateGraphicOverlayImageSourceInfo;
    private CameraSelector cameraSelector;

    // Settings
    SharedPreferences settings;

    // Load libraries
    public static final String OPENCV_LIBRARY_NAME = "opencv_java4";
    public static final String PRESENTER_LIBRARY_NAME = "presenter";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recover the instance state.
        if (savedInstanceState != null) {
            lensFacing = savedInstanceState.getInt(LENS_FACING_KEY);
            settings = getSharedPreferences("Settings", MODE_PRIVATE);
        } else {
            // set default shared settings
            settings = getSharedPreferences("Settings", MODE_PRIVATE);
            SharedPreferences.Editor prefEditor = settings.edit();
            prefEditor.putInt("Threads", 4);
            prefEditor.putString("Device", "CPU");
            prefEditor.putString("Algorithm", "MLKit");
            prefEditor.putBoolean("AllowGrow", false);
            prefEditor.apply();
        }

        Log.d(TAG, "Start face_recognition application");
        setContentView(R.layout.activity_main);

        try{
            System.loadLibrary(OPENCV_LIBRARY_NAME);
            Log.i(TAG, "Load OpenCV library");
        } catch (UnsatisfiedLinkError e) {
            Log.e("UnsatisfiedLinkError",
                    "Failed to load native OpenCV libraries\n" + e.toString());
            System.exit(1);
        }
        try{
            System.loadLibrary(PRESENTER_LIBRARY_NAME);
            Log.i(TAG, "Load presenter library");
        } catch (UnsatisfiedLinkError e) {
            Log.e("UnsatisfiedLinkError",
                    "Failed to load native filters libraries\n" + e.toString());
            System.exit(1);
        }

        editMode = (ImageButton) findViewById(R.id.gallery_button);
        switchCamera = (ImageButton) findViewById(R.id.switch_camera);
        settingsButton = (ImageButton) findViewById(R.id.settings);

//        previewView = findViewById(R.id.preview_view);
//        if (previewView == null) {
//            Log.d(TAG, "previewView is null");
//        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        if(allPermissionsGranted()){
            new ViewModelProvider(this)
                    .get(CameraXViewModel.class)
                    .getProcessCameraProvider()
                    .observe(
                            this,
                            provider -> {
                                cameraProvider = provider;
                                try {
                                    bindAllCameraUseCases();
                                } catch (IOException | URISyntaxException e) {
                                    throw new RuntimeException(e);
                                }
                            });
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(LENS_FACING_KEY, lensFacing);;

        // Call superclass to save any view hierarchy.
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                new ViewModelProvider(this)
                        .get(CameraXViewModel.class)
                        .getProcessCameraProvider()
                        .observe(
                                this,
                                provider -> {
                                    cameraProvider = provider;
                                    try {
                                        bindAllCameraUseCases();
                                    } catch (IOException | URISyntaxException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    public void setAllowGrow(View view) {
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("AllowGrow", true);
        prefEditor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            bindAllCameraUseCases();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    private void bindAllCameraUseCases() throws IOException, URISyntaxException {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            //bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder().setTargetResolution(new Size(1280, 960));

        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewUseCase);
    }
    private void bindAnalysisUseCase() throws IOException, URISyntaxException {
        if (cameraProvider == null) {
            return;
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        Log.i(TAG, "Using Face Detector Processor");
        if (settings.getString("Algorithm", "MLKit").equals("MLKit")) {
            imageProcessor = new FaceDetectorProcessor(this);
        } else {
            imageProcessor = new RecognizerProcessor(this);
        }

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9);
        analysisUseCase = builder.build();

        needUpdateGraphicOverlayImageSourceInfo = true;
        analysisUseCase.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
                        } else {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
                        }
                        needUpdateGraphicOverlayImageSourceInfo = false;
                    }

                    imageProcessor.processImageProxy(imageProxy, graphicOverlay);
                });

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisUseCase);
    }

    public void setSwitchCamera(View view) {
        if (cameraProvider == null) {
            return;
        }
        int newLensFacing =
                lensFacing == CameraSelector.LENS_FACING_FRONT
                        ? CameraSelector.LENS_FACING_BACK
                        : CameraSelector.LENS_FACING_FRONT;
        CameraSelector newCameraSelector =
                new CameraSelector.Builder().requireLensFacing(newLensFacing).build();
        try {
            if (cameraProvider.hasCamera(newCameraSelector)) {
                Log.d(TAG, "Set facing to " + newLensFacing);
                lensFacing = newLensFacing;
                cameraSelector = newCameraSelector;
                bindAllCameraUseCases();
                return;
            }
        } catch (CameraInfoUnavailableException e) {
            // Falls through
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Toast.makeText(
                        getApplicationContext(),
                        "This device does not have lens with facing: " + newLensFacing,
                        Toast.LENGTH_SHORT)
                .show();
    }

    public void openSettings(View view) {
        Intent newWindow = new Intent(this, Settings.class);
        startActivity(newWindow);
    }
}

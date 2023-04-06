package com.yadro.mlkit_detector;

import static com.yadro.utils.BitmapUtils.rotateBitmap;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.yadro.face_recognition_app.Presenter;
import com.yadro.face_recognition_app.ScopedExecutor;
import com.yadro.face_recognition_app.VisionImageProcessor;
import com.yadro.graphics.CameraImageGraphic;
import com.yadro.graphics.GraphicOverlay;
import com.yadro.graphics.InferenceInfoGraphic;
import com.yadro.utils.BitmapUtils;
import com.yadro.utils.FrameMetadata;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

/**
 * Abstract base class for vision frame processors. Subclasses need to implement {@link
 * #onSuccess(Bitmap, Object, GraphicOverlay)} to define what they want to with the detection results and
 * {@link #detectInImage(Bitmap)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
public abstract class VisionProcessorBase<T> implements VisionImageProcessor {

    protected static final String MANUAL_TESTING_LOG = "LogTagForTest";
    private static final String TAG = "VisionProcessorBase";

    private final ActivityManager activityManager;
    private final Timer fpsTimer = new Timer();
    private final ScopedExecutor executor;
    private Presenter presenter;
    private Context parentContext;
    public static final String PRESENTER_LIBRARY_NAME = "presenter";

    // Whether this processor is already shut down
    private boolean isShutdown;

    // Used to calculate latency, running in the same thread, no sync needed.
    private int numRuns = 0;
    private long totalFrameMs = 0;
    private long maxFrameMs = 0;
    private long minFrameMs = Long.MAX_VALUE;
    private long totalDetectorMs = 0;
    private long maxDetectorMs = 0;
    private long minDetectorMs = Long.MAX_VALUE;

    // Frame count that have been processed so far in an one second interval to calculate FPS.
    private int frameProcessedInOneSecondInterval = 0;
    private int framesPerSecond = 0;

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private ByteBuffer latestImage;

    @GuardedBy("this")
    private FrameMetadata latestImageMetaData;
    // To keep the images and metadata in process.
    @GuardedBy("this")
    private ByteBuffer processingImage;

    @GuardedBy("this")
    private FrameMetadata processingMetaData;

    protected VisionProcessorBase(Context context) {
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        parentContext = context;
        executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
        fpsTimer.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        framesPerSecond = frameProcessedInOneSecondInterval;
                        frameProcessedInOneSecondInterval = 0;
                    }
                },
                /* delay= */ 0,
                /* period= */ 1000);
        try{
            System.loadLibrary(PRESENTER_LIBRARY_NAME);
            Log.i(TAG, "Load presenter library");
        } catch (UnsatisfiedLinkError e) {
            Log.e("UnsatisfiedLinkError",
                    "Failed to load native filters libraries\n" + e.toString());
            System.exit(1);
        }
        presenter = new Presenter("", 0);
    }

    // -----------------Code for processing single still image----------------------------------------
    @Override
    public void processBitmap(Bitmap bitmap, final GraphicOverlay graphicOverlay) {
        long frameStartMs = SystemClock.elapsedRealtime();

        requestDetectInImage(
                bitmap,
                graphicOverlay,
                /* originalCameraImage= */ null,
                /* shouldShowFps= */ false);
    }

    // -----------------Code for processing live preview frame from CameraX API-----------------------

    @ExperimentalGetImage
    private Bitmap toBitmap(Image image, int rotation) {
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
        return rotateBitmap(bmp, rotation, false, false);
    }

    @Override
    @RequiresApi(VERSION_CODES.LOLLIPOP)
    @ExperimentalGetImage
    public void processImageProxy(Bitmap bitmap, GraphicOverlay graphicOverlay) {
//        if (isShutdown) {
//            image.close();
//            return;
//        }

        Task<T> task = requestDetectInImage(
                bitmap,
                graphicOverlay,
                /* originalCameraImage= */ bitmap,
                /* shouldShowFps= */ true);
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
        while (!task.isComplete());
    }

    // -----------------Common processing logic-------------------------------------------------------
    private Task<T> requestDetectInImage(
            final Bitmap image,
            final GraphicOverlay graphicOverlay,
            @Nullable final Bitmap originalCameraImage,
            boolean shouldShowFps) {
        return setUpListener(
                detectInImage(image), graphicOverlay, originalCameraImage, shouldShowFps);
    }

    private Task<T> setUpListener(
            Task<T> task,
            final GraphicOverlay graphicOverlay,
            @Nullable final Bitmap originalCameraImage,
            boolean shouldShowFps) {
        final long detectorStartMs = SystemClock.elapsedRealtime();
        return task.addOnSuccessListener(
                        results -> {
//                            long endMs = SystemClock.elapsedRealtime();
//                            long currentDetectorLatencyMs = endMs - detectorStartMs;
//                            if (numRuns >= 500) {
//                                resetLatencyStats();
//                            }
//                            numRuns++;
//                            frameProcessedInOneSecondInterval++;
//                            totalFrameMs += currentFrameLatencyMs;
//                            maxFrameMs = max(currentFrameLatencyMs, maxFrameMs);
//                            minFrameMs = min(currentFrameLatencyMs, minFrameMs);
//                            totalDetectorMs += currentDetectorLatencyMs;
//                            maxDetectorMs = max(currentDetectorLatencyMs, maxDetectorMs);
//                            minDetectorMs = min(currentDetectorLatencyMs, minDetectorMs);
//
//                            // Only log inference info once per second. When frameProcessedInOneSecondInterval is
//                            // equal to 1, it means this is the first frame processed during the current second.
//                            if (frameProcessedInOneSecondInterval == 1) {
//                                Log.d(TAG, "Num of Runs: " + numRuns);
//                                Log.d(
//                                        TAG,
//                                        "Frame latency: max="
//                                                + maxFrameMs
//                                                + ", min="
//                                                + minFrameMs
//                                                + ", avg="
//                                                + totalFrameMs / numRuns);
//                                Log.d(
//                                        TAG,
//                                        "Detector latency: max="
//                                                + maxDetectorMs
//                                                + ", min="
//                                                + minDetectorMs
//                                                + ", avg="
//                                                + totalDetectorMs / numRuns);
//                                MemoryInfo mi = new MemoryInfo();
//                                activityManager.getMemoryInfo(mi);
//                                long availableMegs = mi.availMem / 0x100000L;
//                                Log.d(TAG, "Memory available in system: " + availableMegs + " MB");
//                            }

//                            graphicOverlay.clear();
//                            if (originalCameraImage != null) {
//                                graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage, presenter));
//                            }
                            VisionProcessorBase.this.onSuccess(originalCameraImage, results, graphicOverlay);
                        })
                .addOnFailureListener(
                        e -> {
                            graphicOverlay.clear();
                            graphicOverlay.postInvalidate();
                            String error = "Failed to process. Error: " + e.getLocalizedMessage();
                            Toast.makeText(
                                            graphicOverlay.getContext(),
                                            error + "\nCause: " + e.getCause(),
                                            Toast.LENGTH_SHORT)
                                    .show();
                            Log.d(TAG, error);
                            e.printStackTrace();
                            VisionProcessorBase.this.onFailure(e);
                        });
    }

    @Override
    public void stop() {
        executor.shutdown();
        isShutdown = true;
        resetLatencyStats();
        fpsTimer.cancel();
    }

    private void resetLatencyStats() {
        numRuns = 0;
        totalFrameMs = 0;
        maxFrameMs = 0;
        minFrameMs = Long.MAX_VALUE;
        totalDetectorMs = 0;
        maxDetectorMs = 0;
        minDetectorMs = Long.MAX_VALUE;
    }

    protected abstract Task<T> detectInImage(Bitmap image);

    protected abstract void onSuccess(@NonNull Bitmap originalCameraImage, @NonNull T results, @NonNull GraphicOverlay graphicOverlay);

    protected abstract void onFailure(@NonNull Exception e);
}

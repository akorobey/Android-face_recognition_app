package com.yadro.face_recognition_app;

import android.app.Application;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

/** View model for interacting with CameraX. */
@RequiresApi(VERSION_CODES.LOLLIPOP)
public final class CameraXViewModel extends AndroidViewModel {

    private static final String TAG = "CameraXViewModel";
    private MutableLiveData<ProcessCameraProvider> cameraProviderLiveData;

    /**
     * Create an instance which interacts with the camera service via the given application context.
     */
    public CameraXViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<ProcessCameraProvider> getProcessCameraProvider() {
        if (cameraProviderLiveData == null) {
            cameraProviderLiveData = new MutableLiveData<>();

            ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                    ProcessCameraProvider.getInstance(getApplication());
            cameraProviderFuture.addListener(
                    () -> {
                        try {
                            cameraProviderLiveData.setValue(cameraProviderFuture.get());
                        } catch (ExecutionException | InterruptedException e) {
                            // Handle any errors (including cancellation) here.
                            Log.e(TAG, "Unhandled exception", e);
                        }
                    },
                    ContextCompat.getMainExecutor(getApplication()));
        }

        return cameraProviderLiveData;
    }
}

package com.example.face_recognition_app;

import org.opencv.core.Mat;

public class Presenter {
    private long presenterNativeObj;
    private native long getPresenter(String keys);
    public Presenter(String keys) {
        presenterNativeObj = getPresenter(keys);
    }

    private native void drawGraphs_(long presenterAddr, long matAddr);

    public void drawGraphs(Mat frame) {
        drawGraphs_(presenterNativeObj, frame.getNativeObjAddr());
    }

    private native void handleKey_(long presenterAddr, int key);
    public void handleKey(int key) {
        handleKey_(presenterNativeObj, key);
    };

    @Override
    protected void finalize() throws Throwable {
        delete(presenterNativeObj);
        super.finalize();
    }
    protected native void delete(long nativeObj);
}

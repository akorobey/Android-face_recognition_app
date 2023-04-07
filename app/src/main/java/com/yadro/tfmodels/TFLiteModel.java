package com.yadro.tfmodels;

import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

abstract public class TFLiteModel<T> {
    protected static final String TAG = "TFLiteModel";

    protected MappedByteBuffer model;
    protected Interpreter interpreter;
    protected final Interpreter.Options options = new Interpreter.Options();
    protected final CompatibilityList compatList = new CompatibilityList();
    protected int nthreads;
    protected String device;
    public int imageWidth;
    public int imageHeight;
    public int inputWidth;
    public int inputHeight;
    protected ImageProcessor imgProcessor;
    protected ArrayList<String> outputNames = new ArrayList<String>();
    protected ArrayList<DataType> outputDataTypes  = new ArrayList<DataType>();
    protected ArrayList<int[]> outputShapes  = new ArrayList<int[]>();

    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        Log.i(TAG, "Load asset model file: " + modelPath);
        File file = new File(modelPath);
        FileInputStream inputStream = new FileInputStream(file);
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
    }

    public TFLiteModel(final String modelFile, final String device, final int nthreads) {
        this.nthreads = nthreads;
        this.device = device;
        try {
            model = loadModelFile(modelFile);
        } catch (IOException ex) {
            Log.e("IO Error",
                    "Failed to load model asset file" + ex.toString());
            System.exit(1);
        }

        readModel();
    }

    protected void readModel() throws IllegalArgumentException {
        Log.i(TAG, "Reading model");
        if(device.equals("GPU")){
            // if the device has a supported GPU, add the GPU delegate
            GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
            Log.i(TAG, "Delegate: GPU");
        } else if (device.equals("CPU")) {
            // if the GPU is not supported, run on CPU with specified number of threads
            options.setNumThreads(nthreads);
            options.setUseXNNPACK(true);
            Log.i(TAG, "Delegate: CPU");
        } else {
            throw new IllegalArgumentException("Unknown device provided: " + device);
        }

        interpreter = new Interpreter(model, options);
    }
    abstract protected void getInputsOutputsInfo();

    abstract protected TensorImage preprocess(Bitmap bitmap);
    abstract public T run(Bitmap bitmap);
    abstract protected T postprocess();
}

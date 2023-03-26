package com.example.face_recognition_app;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class FaceRecognitionModel extends TFLiteModel<ArrayList<Float>> {
    private TensorBuffer embeddingsBuffer;
    private final TensorProcessor tensorProcessor;

    private float[] mean = {127.5f, 127.5f, 127.5f};
    private float[] scale = {127.5f, 127.5f, 127.5f};
    int numEmbeddings = 192;

    ArrayList<float[]> anchors = new ArrayList<>();


    public FaceRecognitionModel(final String modelFile, final int nthreads) {
        super(modelFile, nthreads);
        getInputsOutputsInfo();
        imgProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
                        .add(new NormalizeOp(mean, scale))
                        .build();
        embeddingsBuffer = TensorBuffer.createFixedSize(outputShapes.get(0), outputDataTypes.get(0));
        TensorProcessor.Builder builder = new TensorProcessor.Builder();
        tensorProcessor = builder.build();
    }

    @Override
    protected void getInputsOutputsInfo() {
        int inputsCount = interpreter.getInputTensorCount();
        Log.i(TAG, "Inputs:");
        Tensor tensor = interpreter.getInputTensor(0);
        Log.i(TAG, "\t" + tensor.name() + ": " + String.valueOf(tensor.dataType()) + " " + Arrays.toString(tensor.shape()));
        inputWidth = tensor.shape()[2];
        inputHeight = tensor.shape()[1];

        int outputsCount = interpreter.getOutputTensorCount();
        Log.i(TAG, "Outputs: ");
        for (int i = 0; i < outputsCount; ++i) {
            tensor = interpreter.getOutputTensor(i);
            outputNames.add(tensor.name());
            outputDataTypes.add(tensor.dataType());
            outputShapes.add(tensor.shape());
            Log.i(TAG, "\t" + tensor.name() + ": " + String.valueOf(tensor.dataType()) + " " + Arrays.toString(tensor.shape()));
        }
    }

    @Override
    protected final TensorImage preprocess(Bitmap bitmap) {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        tensorImage = imgProcessor.process(tensorImage);
        return tensorImage;
    }

    @Override
    public ArrayList<Float> run(Bitmap bitmap) {
        imageWidth = bitmap.getWidth();
        imageHeight = bitmap.getHeight();
        TensorImage tensorImage = preprocess(bitmap);
        Object input = tensorImage.getBuffer();
        Object output = embeddingsBuffer.getBuffer().rewind();
        interpreter.run(input, output);
        return postprocess();
    }

    @Override
    protected ArrayList<Float> postprocess() {
        float[] embeddings = tensorProcessor.process(embeddingsBuffer).getFloatArray();
        ArrayList<Float> result = new ArrayList<>();
        for (int i = 0; i < numEmbeddings; ++i) {
            result.add(embeddings[i]);
        }
        return result;
    }
}

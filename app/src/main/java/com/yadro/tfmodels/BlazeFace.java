package com.yadro.tfmodels;

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
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class BlazeFace extends TFLiteModel<ArrayList<BBox>> {
    private TensorBuffer boxesBuffer;

    private TensorBuffer scoresBuffer;
    private final TensorProcessor tensorProcessor;

    private float[] mean = {127.5f, 127.5f, 127.5f};
    private float[] scale = {127.5f, 127.5f, 127.5f};
    double confidenceThreshold = 0.5;
    double logitThreshold = log(confidenceThreshold / (1.0 - confidenceThreshold));
    int numLayers = 4;
    int numBoxes = 896;
    int numCoordinates = 16;
    float anchorOffsetX = 0.5f;
    float anchorOffsetY = 0.5f;
    int[] strides = {8, 16, 16, 16};
    float interpolatedScaleAspectRatio = 1.0f;

    ArrayList<float[]> anchors = new ArrayList<>();

    private void generateAnchors() {
        int layerId = 0;
        while (layerId < numLayers) {
            int lastSameStrideLayer = layerId;
            int repeats = 0;
            while (lastSameStrideLayer < numLayers &&
                    strides[lastSameStrideLayer] == strides[layerId]) {
                lastSameStrideLayer += 1;
                repeats += 2;
            }
            int stride = strides[layerId];
            int featureMapHeight = inputHeight / stride;
            int featureMapWidth = inputWidth / stride;
            for (int y = 0; y < featureMapHeight; ++y) {
                float yCenter =
                        (y + anchorOffsetY) / featureMapHeight;
                for (int x = 0; x < featureMapWidth; ++x) {
                    float xCenter =
                            (x + anchorOffsetX) / featureMapWidth;
                    for (int i = 0; i < repeats; ++i) {
                        anchors.add(new float[]{xCenter, yCenter});
                    }
                }
            }

            layerId = lastSameStrideLayer;
        }
    }

    public BlazeFace(final String modelFile, final String device, final int nthreads) {
        super(modelFile, device, nthreads);
        getInputsOutputsInfo();
        generateAnchors();
        imgProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeLetterbox(inputHeight, inputWidth))
                        .add(new NormalizeOp(mean, scale))
                        .build();
        boxesBuffer = TensorBuffer.createFixedSize(outputShapes.get(0), outputDataTypes.get(0));
        scoresBuffer = TensorBuffer.createFixedSize(outputShapes.get(1), outputDataTypes.get(1));
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

    public final ArrayList<BBox> run(Bitmap bitmap) {
        imageWidth = bitmap.getWidth();
        imageHeight = bitmap.getHeight();
        TensorImage tensorImage = preprocess(bitmap);
        Object[] inputs = new Object[]{tensorImage.getBuffer()};
        Map<Integer, Object> outputs = new HashMap();
        outputs.put(0, boxesBuffer.getBuffer().rewind());
        outputs.put(1, scoresBuffer.getBuffer().rewind());
        interpreter.runForMultipleInputsOutputs(inputs, outputs);
        return postprocess();
    }

    private void decodeBox(float[] boxes, int boxId) {
        float scale = inputHeight;
        int numPoints = numCoordinates / 2;
        int startPos = boxId * numCoordinates;

        for (int j = 0; j < numPoints; ++j) {
            boxes[startPos + 2 * j] = boxes[startPos + 2 * j] / scale;
            boxes[startPos + 2 * j + 1] = boxes[startPos + 2 * j + 1] / scale;
            if (j != 1) {
                boxes[startPos + 2 * j] += anchors.get(boxId)[0];
                boxes[startPos + 2 * j + 1] += anchors.get(boxId)[1];
            }
        }

        // convert x center, y center, w, h to xmin, ymin, xmax, ymax
        float halfWidth = boxes[startPos + 2] / 2;
        float halfHeight = boxes[startPos + 3] / 2;
        float xCenter = boxes[startPos];
        float yCenter = boxes[startPos + 1];

        boxes[startPos] -= halfWidth;
        boxes[startPos + 1] -= halfHeight;

        boxes[startPos + 2] = xCenter + halfWidth;
        boxes[startPos + 3] = yCenter + halfHeight;
    }


    ArrayList<BBox> getDetections(float[] scores, float[] boxes) {
        ArrayList<BBox> detections = new ArrayList<BBox>();
        for (int boxId = 0; boxId < numBoxes; ++boxId) {
            float score = scores[boxId];
            if (score < logitThreshold) {
                continue;
            }
            BBox object = new BBox();
            object.confidence = (float) (1.f / (1.f + exp(-score)));

            decodeBox(boxes, boxId);

            int startPos = boxId * numCoordinates;
            PointF topLeft = new PointF(boxes[startPos] * inputWidth, boxes[startPos + 1] * inputHeight);
            PointF rightBottom = new PointF(boxes[startPos + 2] * inputWidth, boxes[startPos + 3] * inputHeight);
            PointF leftEye = new PointF(boxes[startPos + 4] * inputWidth, boxes[startPos + 5] * inputHeight);
            PointF rightEye = new PointF(boxes[startPos + 6] * inputWidth, boxes[startPos + 7] * inputHeight);
            PointF nose = new PointF(boxes[startPos + 8] * inputWidth, boxes[startPos + 9] * inputHeight);
            PointF mouth = new PointF(boxes[startPos + 10] * inputWidth, boxes[startPos + 11] * inputHeight);

            topLeft = imgProcessor.inverseTransform(topLeft, imageHeight, imageWidth);
            rightBottom= imgProcessor.inverseTransform(rightBottom, imageHeight, imageWidth);
            leftEye = imgProcessor.inverseTransform(leftEye, imageHeight, imageWidth);
            rightEye= imgProcessor.inverseTransform(rightEye, imageHeight, imageWidth);
            nose = imgProcessor.inverseTransform(nose, imageHeight, imageWidth);
            mouth= imgProcessor.inverseTransform(mouth, imageHeight, imageWidth);

            object.face = new Rect(Math.round(topLeft.x), Math.round(topLeft.y), Math.round(rightBottom.x), Math.round(rightBottom.y));

            object.leftEye = leftEye;
            object.rightEye = rightEye;

            detections.add(object);
        }

        return detections;
    }

    ArrayList<Integer> nms(ArrayList<BBox> boxes, ArrayList<Float> scores, final float thresh) {
        ArrayList<Float> areas = new ArrayList<>();
        for (int i = 0; i < boxes.size(); ++i) {
            areas.add((float)(boxes.get(i).face.right - boxes.get(i).face.left) * (boxes.get(i).face.bottom - boxes.get(i).face.top));
        }
        ArrayList<Integer> order = new ArrayList<>();
        for (int i = 0; i < scores.size(); ++i) {
            order.add(i);
        }
        Comparator<Integer> cmp = (o1, o2) -> scores.get(o2).compareTo(scores.get(o1));
        Collections.sort(order, cmp);

        int ordersNum = 0;
        for (; ordersNum < order.size() && scores.get(order.get(ordersNum)) >= 0; ordersNum++);

        ArrayList<Integer> keep = new ArrayList<>();;
        Boolean shouldContinue = true;
        for (int i = 0; shouldContinue && i < ordersNum; ++i) {
            int idx1 = order.get(i);
            if (idx1 >= 0) {
                keep.add(idx1);
                shouldContinue = false;
                for (int j = i + 1; j < ordersNum; ++j) {
                    int idx2 = order.get(j);
                    if (idx2 >= 0) {
                        shouldContinue = true;
                        float overlappingWidth = min(boxes.get(idx1).face.right, boxes.get(idx2).face.right) - max(boxes.get(idx1).face.left, boxes.get(idx2).face.left);
                        float overlappingHeight = min(boxes.get(idx1).face.bottom, boxes.get(idx2).face.bottom) - max(boxes.get(idx1).face.top, boxes.get(idx2).face.top);
                        float intersection = overlappingWidth > 0 && overlappingHeight > 0 ? overlappingWidth * overlappingHeight : 0;
                        float overlap = intersection / (areas.get(idx1) + areas.get(idx2) - intersection);

                        if (overlap >= thresh) {
                            order.set(j, -1);
                        }
                    }
                }
            }
        }
        return keep;
    }
    @Override
    protected ArrayList<BBox> postprocess() {
        float[] scores = tensorProcessor.process(scoresBuffer).getFloatArray();
        float[] boxes = tensorProcessor.process(boxesBuffer).getFloatArray();
        ArrayList<BBox> bboxes = getDetections(scores, boxes);
        ArrayList<Float> filteredScores = new ArrayList<>();
        for (BBox b : bboxes) {
            filteredScores.add(b.confidence);
        }

        ArrayList<Integer> keep = nms(bboxes, filteredScores, 0.6f);
        ArrayList<BBox> result = new ArrayList<>();
        for (int i : keep) {
            result.add(bboxes.get(i));
        }
        return result;
    }
}

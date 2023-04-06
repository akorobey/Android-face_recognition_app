package com.yadro.graphics;

import static com.google.common.primitives.Floats.max;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.yadro.gallery.FaceGallery;
import com.yadro.tfmodels.Face;

public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 8.0f;
    private static final float ID_TEXT_SIZE = 30.0f;
    private static final float ID_Y_OFFSET = 40.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private static final int NUM_COLORS = 2;
    private static final int[][] COLORS =
            new int[][] {
                    // {Text color, background color}
                    {Color.WHITE, Color.RED},
                    {Color.BLACK, Color.GREEN}
            };

    private final Paint facePositionPaint;
    private final Paint[] idPaints;
    private final Paint[] boxPaints;
    private final Paint[] labelPaints;

    private Face face;

    public FaceGraphic(GraphicOverlay overlay, Face face) {
        super(overlay);

        this.face = face;
        final int selectedColor = Color.WHITE;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(selectedColor);

        int numColors = COLORS.length;
        idPaints = new Paint[numColors];
        boxPaints = new Paint[numColors];
        labelPaints = new Paint[numColors];
        for (int i = 0; i < numColors; i++) {
            idPaints[i] = new Paint();
            idPaints[i].setColor(COLORS[i][0] /* text color */);
            idPaints[i].setTextSize(ID_TEXT_SIZE);

            boxPaints[i] = new Paint();
            boxPaints[i].setColor(COLORS[i][1] /* background color */);
            boxPaints[i].setStyle(Paint.Style.STROKE);
            boxPaints[i].setStrokeWidth(BOX_STROKE_WIDTH);

            labelPaints[i] = new Paint();
            labelPaints[i].setColor(COLORS[i][1] /* background color */);
            labelPaints[i].setStyle(Paint.Style.FILL);
        }
    }

    /** Draws the face annotations for position on the supplied canvas. */
    @Override
    public void draw(Canvas canvas) {
        Face face = this.face;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.face.centerX());
        float y = translateY(face.face.centerY());

        // Calculate positions.
        float left = x - scale(face.face.width() / 2.0f);
        float top = y - scale(face.face.height() / 2.0f);
        float right = x + scale(face.face.width() / 2.0f);
        float bottom = y + scale(face.face.height() / 2.0f);

        float lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH;
        float yLabelOffset = - lineHeight;

        // Decide color based on face ID
        int colorID = 1;
        if (face.label.equals(FaceGallery.unknownLabel)) {
            colorID = 0;
        }

        // Calculate width and height of label box
        float textWidth = idPaints[colorID].measureText(face.label);


        // Draw labels
        drawRect(
                canvas,
                left - BOX_STROKE_WIDTH,
                top + yLabelOffset,
                clip(left, 0, canvas.getWidth()) + (2 * BOX_STROKE_WIDTH) + textWidth,
                max(top, lineHeight),
                labelPaints[colorID]);
        yLabelOffset += ID_TEXT_SIZE;
        drawRect(canvas, left, top, right, bottom, boxPaints[colorID]);
        drawText(canvas, face.label, left, max(top, lineHeight) - BOX_STROKE_WIDTH / 2, idPaints[colorID]);
        yLabelOffset += lineHeight;

    }

}

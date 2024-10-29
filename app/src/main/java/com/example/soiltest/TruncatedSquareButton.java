package com.example.soiltest;// TruncatedSquareButton.java
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class TruncatedSquareButton extends View {

    private Paint paint;
    private Path path;

    public TruncatedSquareButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFFFFFFFF); // White color for the fill
        paint.setStyle(Paint.Style.FILL);

        path = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Get width and height of the button
        float width = getWidth();
        float height = getHeight();
        float radius = width / 2; // Radius of the quarter circle to cut out

        // Create path for a square with a quarter circle cutout
        path.reset();
        path.moveTo(0, 0);
        path.lineTo(width, 0);
        path.lineTo(width, height);
        path.lineTo(0, height);
        path.close();

        // Cut out the top-right quarter circle
        path.moveTo(width, 0);
        path.arcTo(width - radius, 0, width, radius, -90, -90, false);

        canvas.drawPath(path, paint);

        // Optional: Draw a border
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF000000); // Black color for the border
        paint.setStrokeWidth(4);
        canvas.drawPath(path, paint);
    }
}

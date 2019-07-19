package com.tomclaw.design;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public class BubbleBitmapDrawable extends Drawable {

    private static int HORIZONTAL_OFFSET;

    private static int BUBBLE_RADIUS;

    private static Path TRIANGLE_LEFT;
    private static Path TRIANGLE_RIGHT;
    private static Path TMP_TRIANGLE_PATH = new Path();

    private final Paint paint = new Paint();
    private final RectF outerBounds = new RectF();
    private final RectF drawingRect = new RectF();

    private final Corner corner;

    private final int bitmapWidth;
    private final int bitmapHeight;
    private final BitmapShader bitmapShader;
    private final Matrix shaderMatrix = new Matrix();

    public BubbleBitmapDrawable(Bitmap bitmap, Corner cornerType, Context context) {
        HORIZONTAL_OFFSET = dp(8, context);
        BUBBLE_RADIUS = dp(6, context);
        TRIANGLE_LEFT = createTriangle(true);
        TRIANGLE_RIGHT = createTriangle(false);

        corner = cornerType;
        if (bitmap != null) {
            bitmapShader = new BitmapShader(bitmap, Shader.TileMode.MIRROR, Shader.TileMode.CLAMP);
            bitmapWidth = bitmap.getWidth();
            bitmapHeight = bitmap.getHeight();
            outerBounds.set(0, 0, bitmapWidth, bitmapHeight);
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            paint.setShader(bitmapShader);
            updateShaderMatrix();
        } else {
            bitmapShader = null;
            bitmapWidth = 0;
            bitmapHeight = 0;
        }
    }

    private void updateShaderMatrix() {
        if (bitmapShader == null) {
            return;
        }
        shaderMatrix.reset();
        float scale, dx = 0, dy = 0;
        float width = outerBounds.width() - HORIZONTAL_OFFSET;
        float height = outerBounds.height();
        if (bitmapWidth * height > width * bitmapHeight) {
            scale = height / (float) bitmapHeight;
            dx = (width - bitmapWidth * scale) * 0.5f;
        } else {
            scale = width / (float) bitmapWidth;
            dy = (height - bitmapHeight * scale) * 0.5f;
        }
        if (corner == Corner.LEFT) {
            dx += HORIZONTAL_OFFSET;
        }
        shaderMatrix.setScale(scale, scale);
        shaderMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        bitmapShader.setLocalMatrix(shaderMatrix);
    }

    @Override
    public int getIntrinsicWidth() {
        if (corner == Corner.NONE) {
            return bitmapWidth;
        }
        return bitmapWidth + HORIZONTAL_OFFSET;
    }

    @Override
    public int getIntrinsicHeight() {
        return bitmapHeight;
    }

    @Override
    public void draw(Canvas canvas) {
        switch (corner) {
            case LEFT: {
                canvas.drawPath(TRIANGLE_LEFT, paint);
                drawingRect.set(HORIZONTAL_OFFSET, 0, outerBounds.right, outerBounds.bottom);
                break;
            }
            case RIGHT: {
                float right = outerBounds.right - HORIZONTAL_OFFSET;
                drawingRect.set(0, 0, right, outerBounds.bottom);
                TRIANGLE_RIGHT.offset(right, 0, TMP_TRIANGLE_PATH);
                canvas.drawPath(TMP_TRIANGLE_PATH, paint);
                break;
            }
            case NONE:
            default:
                drawingRect.set(outerBounds);
        }
        canvas.drawRoundRect(drawingRect, BUBBLE_RADIUS, BUBBLE_RADIUS, paint);
    }

    @Override
    protected void onBoundsChange(Rect newBounds) {
        super.onBoundsChange(newBounds);

        outerBounds.set(newBounds);
        updateShaderMatrix();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public void setDither(boolean dither) {
        paint.setDither(dither);
        invalidateSelf();
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        paint.setFilterBitmap(filter);
        invalidateSelf();
    }

    private Path createTriangle(boolean left) {
        float cornerX, wideX;
        Path p = new Path();
        if (left) {
            cornerX = 0;
            wideX = HORIZONTAL_OFFSET;
            p.moveTo(wideX * 2, 0);
            p.lineTo(cornerX, 0);
            p.lineTo(wideX, HORIZONTAL_OFFSET);
            p.moveTo(wideX * 2, HORIZONTAL_OFFSET);
        } else {
            cornerX = HORIZONTAL_OFFSET;
            wideX = 0;
            p.moveTo(wideX - HORIZONTAL_OFFSET, 0);
            p.lineTo(cornerX, 0);
            p.lineTo(wideX, HORIZONTAL_OFFSET);
            p.moveTo(wideX * 2, HORIZONTAL_OFFSET);
        }
        p.close();
        return p;
    }

    private int dp(float v, Context context) {
        return (int) (v * context.getResources().getDisplayMetrics().density + 0.5);
    }

}


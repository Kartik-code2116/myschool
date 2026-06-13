package com.kartik.myschool.utils.zoom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

public class ZoomLayout extends FrameLayout {

    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 4.0f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private ValueAnimator zoomAnimator;

    private float scale = 1.0f;
    private float translationX = 0f;
    private float translationY = 0f;

    private float lastX;
    private float lastY;
    private float downX;
    private float downY;
    private boolean isDragging = false;
    private int touchSlop;

    public ZoomLayout(Context context) {
        super(context);
        init(context);
    }

    public ZoomLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipChildren(false);
        setClipToPadding(false);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        child.setPivotX(0);
        child.setPivotY(0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            clampAndApplyTransformations();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Always pass touch events to detectors
        scaleDetector.onTouchEvent(ev);
        gestureDetector.onTouchEvent(ev);

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                lastX = ev.getX();
                lastY = ev.getY();
                isDragging = false;
                if (scale > MIN_SCALE) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (scale > MIN_SCALE) {
                    float dx = ev.getX() - downX;
                    float dy = ev.getY() - downY;
                    if (Math.hypot(dx, dy) > touchSlop) {
                        isDragging = true;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        return true; // Start intercepting drag
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (ev.getPointerCount() > 1) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true; // Intercept for pinch zoom
                }
                break;
        }

        return isDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);
        gestureDetector.onTouchEvent(ev);

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastX = ev.getX();
                lastY = ev.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress()) {
                    float dx = ev.getX() - lastX;
                    float dy = ev.getY() - lastY;
                    translationX += dx;
                    translationY += dy;
                    clampAndApplyTransformations();
                }
                lastX = ev.getX();
                lastY = ev.getY();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true;
    }

    private void applyTransformations() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.setPivotX(0);
            child.setPivotY(0);
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setTranslationX(translationX);
            child.setTranslationY(translationY);
        }
    }

    private void clampAndApplyTransformations() {
        int W = getWidth();
        int H = getHeight();
        if (W == 0 || H == 0) return;

        View child = getChildCount() > 0 ? getChildAt(0) : null;
        int w = child != null ? child.getWidth() : W;
        int h = child != null ? child.getHeight() : H;

        float scaledWidth = w * scale;
        float scaledHeight = h * scale;

        if (scaledWidth <= W) {
            translationX = (W - scaledWidth) / 2f;
        } else {
            translationX = Math.min(Math.max(translationX, W - scaledWidth), 0f);
        }

        if (scaledHeight <= H) {
            translationY = (H - scaledHeight) / 2f;
        } else {
            translationY = Math.min(Math.max(translationY, H - scaledHeight), 0f);
        }

        applyTransformations();
    }

    public void zoomTo(float targetScale, float focusX, float focusY, boolean animate) {
        if (getChildCount() == 0) return;

        float startScale = scale;
        float startTx = translationX;
        float startTy = translationY;

        float finalTx;
        float finalTy;

        int W = getWidth();
        int H = getHeight();
        View child = getChildAt(0);
        int w = child.getWidth();
        int h = child.getHeight();

        float scaledWidth = w * targetScale;
        float scaledHeight = h * targetScale;

        if (startScale == 0) startScale = 1.0f;
        float calculatedTx = focusX - ((focusX - startTx) / startScale) * targetScale;
        float calculatedTy = focusY - ((focusY - startTy) / startScale) * targetScale;

        if (scaledWidth <= W) {
            finalTx = (W - scaledWidth) / 2f;
        } else {
            finalTx = Math.min(Math.max(calculatedTx, W - scaledWidth), 0f);
        }

        if (scaledHeight <= H) {
            finalTy = (H - scaledHeight) / 2f;
        } else {
            finalTy = Math.min(Math.max(calculatedTy, H - scaledHeight), 0f);
        }

        if (!animate) {
            scale = targetScale;
            translationX = finalTx;
            translationY = finalTy;
            applyTransformations();
            return;
        }

        if (zoomAnimator != null && zoomAnimator.isRunning()) {
            zoomAnimator.cancel();
        }

        zoomAnimator = ValueAnimator.ofFloat(0f, 1f);
        zoomAnimator.setDuration(250);
        zoomAnimator.setInterpolator(new DecelerateInterpolator());
        final float fStartScale = startScale;
        final float fTargetScale = targetScale;
        final float fStartTx = startTx;
        final float fFinalTx = finalTx;
        final float fStartTy = startTy;
        final float fFinalTy = finalTy;
        zoomAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            scale = fStartScale + (fTargetScale - fStartScale) * fraction;
            translationX = fStartTx + (fFinalTx - fStartTx) * fraction;
            translationY = fStartTy + (fFinalTy - fStartTy) * fraction;
            applyTransformations();
        });
        zoomAnimator.start();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scale;
            scale *= detector.getScaleFactor();
            scale = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            if (oldScale == 0) oldScale = 1.0f;
            translationX = focusX - (focusX - translationX) * (scale / oldScale);
            translationY = focusY - (focusY - translationY) * (scale / oldScale);

            clampAndApplyTransformations();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float targetScale = (scale > MIN_SCALE) ? MIN_SCALE : 2.5f;
            zoomTo(targetScale, e.getX(), e.getY(), true);
            return true;
        }
    }
}

package com.zy.reader.widget.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;

import com.zy.reader.utils.PageFactory;
import com.zy.reader.widget.PageWidget;

public class CoverPageAnimation extends PageAnimation {
    private float firstDownX;
    private float firstDownY;
    private float moveX;
    private int curAction = MotionEvent.ACTION_UP;
    private boolean isAnimation = false;
    private GradientDrawable mBackShadowDrawableLR;
    private int shadowWidth=50;

    public CoverPageAnimation(PageWidget pageWidget) {
        super(pageWidget);
        init();
    }

    private void init() {
        int[] mBackShadowColors = new int[]{0x66000000, 0x00000000};
        mBackShadowDrawableLR = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, mBackShadowColors);
        mBackShadowDrawableLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);
    }

    @Override
    public void draw(Canvas canvas) {
        if (curAction == MotionEvent.ACTION_MOVE || isAnimation) {
            if (moveX < 0) {
                canvas.drawBitmap(pageWidget.getNextPage(), 0, 0, null);
                canvas.drawBitmap(pageWidget.getCurPage(), moveX, 0, null);
                if (!isAnimation||(shadowWidth < Math.abs(moveX) && Math.abs(moveX) <= pageWidget.getWidth() - shadowWidth)){
                    addShadow((int) (pageWidget.getWidth() - Math.abs(moveX)), canvas);
                }
            } else if (moveX == 0) {
                canvas.drawBitmap(pageWidget.getCurPage(), 0, 0, null);
            } else if (moveX > 0) {
                canvas.drawBitmap(pageWidget.getCurPage(), 0, 0, null);
                canvas.drawBitmap(pageWidget.getPrePage(), moveX - pageWidget.getWidth(), 0, null);
                if (!isAnimation||(shadowWidth < Math.abs(moveX) && Math.abs(moveX) <= pageWidget.getWidth() - shadowWidth)){
                    addShadow((int) moveX, canvas);
                }
            }
        } else {
            canvas.drawBitmap(pageWidget.getCurPage(), 0, 0, null);
        }
    }

    public void addShadow(int left, Canvas canvas) {
        mBackShadowDrawableLR.setBounds(left, 0, left + shadowWidth, pageWidget.getHeight());
        mBackShadowDrawableLR.draw(canvas);
    }

    @Override
    public boolean handleTouchEvent(MotionEvent event) {
        if (isAnimation)
            return true;
        curAction = event.getAction();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                firstDownX = event.getRawX();
                firstDownY = event.getRawY();
                moveX = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                moveX = event.getRawX() - firstDownX;
                pageWidget.postInvalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
//                pageWidget.postInvalidate();
                break;
            case MotionEvent.ACTION_UP:
                int width = pageWidget.getWidth();
                if (moveX < 0) {
                    if (Math.abs(moveX) > width / 8f) {
                        nextPage();
                        return true;
                    }
                } else if (moveX == 0) {
                    if (firstDownX < pageWidget.getWidth() / 3f) {
                        prePage();
                        PageFactory.showOrHideOptionLayout(1);
                    } else if (firstDownX < pageWidget.getWidth() * 2 / 3f) {
                        PageFactory.showOrHideOptionLayout(0);
                    } else {
                        nextPage();
                        PageFactory.showOrHideOptionLayout(1);
                    }

                } else {
                    if (Math.abs(moveX) > width / 8f) {
                        prePage();
                        return true;
                    }
                }
                pageWidget.postInvalidate();
                break;
        }

        return true;
    }


    private void prePage() {
        isAnimation = true;
        ValueAnimator valueAnimator = ObjectAnimator.ofFloat(moveX, pageWidget.getWidth());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                moveX = (float) animation.getAnimatedValue();
                pageWidget.postInvalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isAnimation = false;
                PageFactory.prePage();
                pageWidget.postInvalidate();
            }
        });
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setDuration(200);
        valueAnimator.start();
    }


    private void nextPage() {
        isAnimation = true;
        ValueAnimator valueAnimator = ObjectAnimator.ofFloat(moveX, -pageWidget.getWidth());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                moveX = (float) animation.getAnimatedValue();
                pageWidget.postInvalidate();
            }
        });

        valueAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isAnimation = false;
                PageFactory.nextPage();
                pageWidget.postInvalidate();
            }
        });

        valueAnimator.setInterpolator(new LinearInterpolator());

        valueAnimator.setDuration(200);

        valueAnimator.start();

    }

}

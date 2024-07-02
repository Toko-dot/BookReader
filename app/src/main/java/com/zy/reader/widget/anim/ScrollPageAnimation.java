package com.zy.reader.widget.anim;

import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import com.zy.reader.utils.PageFactory;
import com.zy.reader.widget.PageWidget;

public class ScrollPageAnimation extends PageAnimation {

    private ViewConfiguration viewConfiguration;
    private int scaledMinimumFlingVelocity;
    private VelocityTracker velocityTracker;
    private Scroller scroller;
    private float preRawY;
    private float yVelocity;

    private boolean fling;


    public ScrollPageAnimation(PageWidget pageWidget) {
        super(pageWidget);
        scroller = new Scroller(pageWidget.getContext());
        viewConfiguration = ViewConfiguration.get(pageWidget.getContext());
        scaledMinimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();

    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(pageWidget.getPrePage(), 0, -pageWidget.getHeight(), null);
        canvas.drawBitmap(pageWidget.getCurPage(), 0, 0, null);
        canvas.drawBitmap(pageWidget.getNextPage(), 0, pageWidget.getHeight(), null);
    }

    @Override
    public boolean handleTouchEvent(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }

        velocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                preRawY = event.getRawY();
                fling = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float moveY = event.getRawY() - preRawY;
                preRawY = event.getRawY();
                if (Math.abs(pageWidget.getScrollY() - (int) moveY) <= pageWidget.getHeight()) {
                    pageWidget.scrollBy(0, -(int) moveY);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                velocityTracker.computeCurrentVelocity(1000);
                yVelocity = velocityTracker.getYVelocity();
                if (Math.abs(yVelocity) > scaledMinimumFlingVelocity) {
                    fling = true;
                    if (yVelocity > 0) {//下拉
                        scroller.fling(0, pageWidget.getScrollY(), 0, -(int) yVelocity, 0, 0, Integer.MIN_VALUE, pageWidget.getScrollY());
                        pageWidget.postInvalidate();
                    } else {//上划
                        scroller.fling(0, pageWidget.getScrollY(), 0, -(int) yVelocity, 0, 0, pageWidget.getScrollY(), Integer.MAX_VALUE);
                        pageWidget.postInvalidate();
                    }
                } else {
                    PageFactory.showOrHideOptionLayout(0);
                }

                if (velocityTracker != null) {
                    velocityTracker.clear();
                    velocityTracker.recycle();
                    velocityTracker = null;
                }

                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            if (fling) {
                int currY = scroller.getCurrY();
                if (currY <= -pageWidget.getHeight()) {
                    PageFactory.prePage();
                    pageWidget.setScrollY(0);
                    fling = false;
                    pageWidget.postInvalidate();
                } else if (currY > pageWidget.getHeight()) {
                    PageFactory.nextPage();
                    pageWidget.setScrollY(0);
                    fling = false;
                    pageWidget.postInvalidate();
                } else {
                    pageWidget.scrollTo(0, currY);
                    pageWidget.postInvalidate();
                }
            }
        }
    }
}

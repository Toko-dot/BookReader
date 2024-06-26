package com.zy.reader.widget.anim;

import android.graphics.Canvas;
import android.view.MotionEvent;

import com.zy.reader.utils.PageFactory;
import com.zy.reader.widget.PageWidget;

public class NonePageAnimation extends PageAnimation{
    private float firstDownX;
    private float firstDownY;

    public NonePageAnimation(PageWidget pageWidget) {
        super(pageWidget);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(pageWidget.getCurPage(), 0, 0, null);
    }

    @Override
    public boolean handleTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                firstDownX = event.getX();
                firstDownY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (Math.abs(event.getX() - firstDownX) < 100) {
                    int width = pageWidget.getWidth();
                    if (firstDownX < width / 3f) {
                        PageFactory.prePage();
                        PageFactory.ShowOrHideOptionLayout(1);
                    } else if (firstDownX < width * 2 / 3f) {
                        PageFactory.ShowOrHideOptionLayout(0);
                    } else {
                        PageFactory.nextPage();
                        PageFactory.ShowOrHideOptionLayout(1);
                    }
                }
                return true;
        }

        return true;
    }
}

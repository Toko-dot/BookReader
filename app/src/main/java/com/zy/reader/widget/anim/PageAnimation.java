package com.zy.reader.widget.anim;

import android.graphics.Canvas;
import android.view.MotionEvent;

import com.zy.reader.widget.PageWidget;

public abstract class PageAnimation {
    protected PageWidget pageWidget;


    public PageAnimation(PageWidget pageWidget) {
        this.pageWidget = pageWidget;
    }

    public  void draw(Canvas canvas){

    }


    public  boolean handleTouchEvent(MotionEvent event){
        return true;
    }

    public  void computeScroll(){

    }

}

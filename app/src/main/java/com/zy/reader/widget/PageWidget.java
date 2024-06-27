package com.zy.reader.widget;

import static com.zy.reader.utils.PageConfig.PAGE_ANIMATION_TYPE_COVER;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zy.reader.widget.anim.CoverPageAnimation;
import com.zy.reader.widget.anim.NonePageAnimation;
import com.zy.reader.widget.anim.PageAnimation;
import com.zy.reader.utils.PageConfig;
import com.zy.reader.utils.PageFactory;

public class PageWidget extends View {

    //前一页
    protected Bitmap mPrePage;
    //当前页
    protected Bitmap mCurPage;
    //后一页
    protected Bitmap mNextPage;

    protected boolean isMeasure = false;

    private PageAnimation pageAnimation;

    private int pageAnimationType = PageConfig.PAGE_ANIMATION_TYPE_NONE;


    public PageWidget(Context context) {
        super(context);
    }

    public PageWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPages(Bitmap mPrePage, Bitmap mCurPage, Bitmap mNextPage, int pageAnimationType) {
        this.mPrePage = mPrePage;
        this.mCurPage = mCurPage;
        this.mNextPage = mNextPage;
        this.pageAnimationType = pageAnimationType;
        createOrUpdateAnimation();
    }

    public Bitmap getCurPage() {
        return mCurPage;
    }

    public Bitmap getNextPage() {
        return mNextPage;
    }

    public Bitmap getPrePage() {
        return mPrePage;
    }

    private void createOrUpdateAnimation() {
        switch (pageAnimationType) {
            case PAGE_ANIMATION_TYPE_COVER:
                pageAnimation = new CoverPageAnimation(this);
                break;
            default:
                pageAnimation = new NonePageAnimation(this);
                break;
        }
    }

    public void changePageAnimationType(int pageAnimationType) {
        this.pageAnimationType = pageAnimationType;
        createOrUpdateAnimation();
        postInvalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(PageConfig.pageColor);
        if (mCurPage == null
                || mPrePage == null
                || mNextPage == null
                || mCurPage.isRecycled()
                || mPrePage.isRecycled()
                || mNextPage.isRecycled()
        ) {

        } else {
            if (pageAnimation != null) {
                pageAnimation.draw(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (pageAnimation != null) {
            return pageAnimation.handleTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getMeasuredHeight() > 0 && getMeasuredWidth() > 0 && !isMeasure) {
            isMeasure = true;
            PageConfig.pageHeight = getMeasuredHeight();
            PageConfig.pageWidth = getMeasuredWidth();
            PageFactory.measureFinish();
        }
    }


}

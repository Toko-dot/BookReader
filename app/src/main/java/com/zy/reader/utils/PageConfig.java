package com.zy.reader.utils;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class PageConfig {
    public static final int PAGE_ANIMATION_TYPE_NONE = 0;
    public static final int PAGE_ANIMATION_TYPE_COVER = 1;
    public static final float TEXT_SIZE_MIN = 40;
    public static final float TEXT_SIZE_MID = 60;
    public static final float TEXT_SIZE_MAX = 80;
    //屏幕宽度
    public static int screenWidth = 0;
    //屏幕高度
    public static int screenHeight = 0;
    //页面宽度
    public static int pageWidth = 0;
    //页面高度
    public static int pageHeight = 0;

    public static int pagePaddingTop = 40;

    public static int pagePaddingBottom = 40;

    public static int pagePaddingLeft = 40;

    public static int pagePaddingRight = 40;

    public static int pageColor = Color.WHITE;
    //字体大小
    public static float textSize = TEXT_SIZE_MID;
    //字体颜色
    public static int textColor = Color.BLACK;

    //字体大小
    public static float lineSpace = 20;

    public static int pageAnimationType = PAGE_ANIMATION_TYPE_NONE;


    public static void init(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        PageConfig.screenWidth = dm.widthPixels;
        PageConfig.screenHeight = dm.heightPixels;
    }

}

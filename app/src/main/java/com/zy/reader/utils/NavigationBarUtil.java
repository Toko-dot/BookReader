package com.zy.reader.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;

public class NavigationBarUtil {

    // 检查是否存在导航栏
    public static boolean hasNavigationBar(Context context) {
        Resources resources = context.getResources();
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        return id > 0 && resources.getBoolean(id);
    }

    // 隐藏导航栏
    public static void hideNavigationBar(Activity activity) {
        // 隐藏导航栏
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.STATUS_BAR_HIDDEN;
        decorView.setSystemUiVisibility(uiOptions);
    }
}

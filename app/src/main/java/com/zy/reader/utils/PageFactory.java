package com.zy.reader.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;

import com.zy.reader.widget.PageWidget;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PageFactory {
    private final static String TAG = "PageFactory";
    private static WeakReference<Context> mContext;
    //书本路径
    private static String bookPath;
    //缓存目录
    private static String cacheBookDir;

    //显示控件
    private static PageWidget pageWidget;
    //监听事件
    private static Listener mListener;
    //前一页
    private static Bitmap mPrePage;
    //当前页
    private static Bitmap mCurPage;
    //后一页
    private static Bitmap mNextPage;
    //文字画笔
    private static TextPaint mTextPaint;

    private static ArrayList<PageInfo> pageInfoList = new ArrayList<>();

    private static PageInfo mCurPageInfo;

    private static Handler mHandler = new Handler(Looper.getMainLooper());
    private static ExecutorService threadExecutor;

    private static boolean isReady = false;

    public static void init(Context context, PageWidget pageWidget, Listener listener) {
        mContext = new WeakReference<>(context);
        PageFactory.pageWidget = pageWidget;
        PageFactory.mListener = listener;
        PageConfig.init(context);
        //创建缓存目录
        File dir = new File(context.getExternalCacheDir(), "reader");
        if (!dir.exists()) {
            dir.mkdir();
        }
        cacheBookDir = dir.getPath();
        threadExecutor = Executors.newSingleThreadExecutor();
        mCurPageInfo = new PageInfo();
        isReady = false;
    }


    public static void config() {

        //创建bitmap
        if (mPrePage != null && !mPrePage.isRecycled()) {
            mPrePage.recycle();
            mPrePage = null;
        }
        if (mCurPage != null && !mCurPage.isRecycled()) {
            mCurPage.recycle();
            mCurPage = null;
        }
        if (mNextPage != null && !mNextPage.isRecycled()) {
            mNextPage.recycle();
            mNextPage = null;
        }

        mPrePage = Bitmap.createBitmap(PageConfig.pageWidth, PageConfig.pageHeight, Bitmap.Config.RGB_565);
        mCurPage = Bitmap.createBitmap(PageConfig.pageWidth, PageConfig.pageHeight, Bitmap.Config.RGB_565);
        mNextPage = Bitmap.createBitmap(PageConfig.pageWidth, PageConfig.pageHeight, Bitmap.Config.RGB_565);

        switch (PageConfig.pageModule) {
            case PageConfig.PAGE_MODULE_TYPE_1:
                PageConfig.pageColor = Color.WHITE;
                PageConfig.textColor = Color.BLACK;
                break;
            case PageConfig.PAGE_MODULE_TYPE_2:
                PageConfig.pageColor = Color.BLACK;
                PageConfig.textColor = Color.WHITE;
                break;
            case PageConfig.PAGE_MODULE_TYPE_3:
                PageConfig.pageColor = Color.parseColor("#C7EDCC");
                PageConfig.textColor = Color.BLACK;
                break;
        }


        //创建画笔
        mTextPaint = new TextPaint();
        mTextPaint.setTextSize(PageConfig.textSize);
        mTextPaint.setColor(PageConfig.textColor);

        if (pageWidget != null) {
            pageWidget.setPages(mPrePage, mCurPage, mNextPage, PageConfig.pageAnimationType);
        }
    }

    public static void openBook(String path) {
        PageFactory.bookPath = path;
        new Thread(new Runnable() {
            @Override
            public void run() {
                BookUtils.cacheBook(PageFactory.cacheBookDir, PageFactory.bookPath, new BookUtils.OnCacheListener() {
                    @Override
                    public void onSuccess() {
                        isReady = true;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onCacheBookSuccess();
                            }
                        });
                    }

                    @Override
                    public void onError() {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mListener.onCacheBookFail();
                            }
                        });
                    }
                });
            }
        }).start();

    }

    public static void startRead(long startPos) {
        pageInfoList.clear();
        config();
        seekTo(startPos);
    }


    public static void preChapter() {
        if (!isReady) {
            return;
        }
        BookUtils.Chapter chapter = BookUtils.getChapterByPosition(mCurPageInfo.startPos);
        if (chapter == null) {
            return;
        }
        chapter = BookUtils.getChapterByPosition(chapter.startPos - 1);

        if (chapter == null) {
            return;
        }
        seekTo(chapter.startPos);
    }


    public static void nextChapter() {
        if (!isReady) {
            return;
        }
        BookUtils.Chapter chapter = BookUtils.getChapterByPosition(mCurPageInfo.startPos);

        if (chapter == null) {
            return;
        }
        chapter = BookUtils.getChapterByPosition(chapter.endPos + 1);

        if (chapter == null) {
            return;
        }

        seekTo(chapter.startPos);
    }


    //上一页
    public static void prePage() {
        if (!isReady || mCurPageInfo.startPos == 0) {
            return;
        }
        seekTo(mCurPageInfo.startPos - 1);
    }

    //下一页
    public static void nextPage() {
        if (!isReady || mCurPageInfo.endPos == BookUtils.bookLength) {
            return;
        }
        seekTo(mCurPageInfo.endPos + 1);
    }

    public static void seekToWithProgress(int progress) {
        long pos = (long) (BookUtils.bookLength * progress / 100) - 1;
        pos = Math.max(0, pos);
        pos = Math.min(pos, BookUtils.bookLength - 1);
        seekTo(pos);
    }


    private static void seekTo(long position) {
        mListener.onProgress(BookUtils.bookLength == 0 ? 0 : (int) ((position + 1) * 100f / BookUtils.bookLength));

        PageInfo pageInfo = getPageInfo(position);

        if (pageInfo == null) {
            threadExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    loadChapter(position);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startDraw(position);
                            refreshUI();
                        }
                    });
                }
            });
        } else {
            startDraw(position);
            refreshUI();
        }

    }

    private static void startDraw(long startPos) {

        //绘制当前页
        PageInfo pageInfo1 = getPageInfo(startPos);

        if (pageInfo1 == null) {
            cleanPage(mCurPage);
            return;
        } else {
            drawPage(mCurPage, pageInfo1);
        }

        //绘制下一页
        PageInfo pageInfo2 = getPageInfo(pageInfo1.endPos + 1);
        if (pageInfo2 == null) {
            cleanPage(mNextPage);
        } else {
            drawPage(mNextPage, pageInfo2);
        }

        //绘制前一页
        PageInfo pageInfo3 = getPageInfo(pageInfo1.startPos - 1);
        if (pageInfo3 == null) {
            cleanPage(mPrePage);
        } else {
            drawPage(mPrePage, pageInfo3);
        }

        mCurPageInfo = pageInfo1;


    }

    public static void refreshUI() {
        pageWidget.postInvalidate();
    }

    public static void changePageModule(int module) {
        if (!isReady) {
            return;
        }
        PageConfig.pageModule = module;
        config();
        startRead(mCurPageInfo.startPos);
    }


    public static void changePageAnimationType(int type) {
        if (!isReady) {
            return;
        }
        PageConfig.pageAnimationType = type;
        pageWidget.changePageAnimationType(type);
    }


    public static void changeTextSize(float textSize) {
        if (!isReady) {
            return;
        }
        PageConfig.textSize = textSize;
        config();
        startRead(mCurPageInfo.startPos);
    }


    /**
     * 确定宽高
     */
    public static void measureFinish() {
        mListener.onMeasureFinish();
    }

    public static void ShowOrHideOptionLayout(int type) {
        mListener.onShowOrHideOptionLayout(type);
    }

    /**
     * 根据开始位置查询
     *
     * @return
     */
    private static PageInfo getPageInfo(long position) {
        PageInfo pageInfo = null;
        int left = 0;
        int right = pageInfoList.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            PageInfo item = pageInfoList.get(mid);
            if (position >= item.startPos && position <= item.endPos) {
                pageInfo = item;
                break;
            } else if (position > item.endPos) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return pageInfo;
    }

    /**
     * 插入列表
     *
     * @param pageInfo
     */
    private static void insertPageInfo(PageInfo pageInfo) {
        if (!containPageInfo(pageInfo)) {
            if (pageInfoList.isEmpty()) {
                pageInfoList.add(pageInfo);
            } else {
                for (int i = 0; i < pageInfoList.size(); i++) {
                    if (pageInfoList.get(i).startPos < pageInfo.startPos) {
                        if (i == pageInfoList.size() - 1) {
                            pageInfoList.add(pageInfo);
                            break;
                        } else {
                            if (pageInfoList.get(i + 1).startPos < pageInfo.startPos) {

                            } else if (pageInfoList.get(i + 1).startPos > pageInfo.startPos) {
                                pageInfoList.add(i + 1, pageInfo);
                                break;
                            }
                        }
                    } else if (pageInfoList.get(i).startPos > pageInfo.startPos) {
                        pageInfoList.add(i, pageInfo);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 是否包含该项
     *
     * @param pageInfo
     * @return
     */
    private static boolean containPageInfo(PageInfo pageInfo) {
        if (pageInfoList.contains(pageInfo)) {
            return true;
        }
        for (int i = 0; i < pageInfoList.size(); i++) {
            if (pageInfo.startPos == pageInfoList.get(i).startPos) {
                return true;
            }
        }
        return false;
    }


    private static void cleanPage(Bitmap bitmap) {
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(PageConfig.pageColor);
    }

    /**
     * 绘制一页
     *
     * @param bitmap
     * @return
     */
    private static void drawPage(Bitmap bitmap, PageInfo pageInfo) {
        if (pageInfo == null)
            return;
        try {
            List<String> lineList = null;
            long startPos = pageInfo.startPos;
            long endPos = pageInfo.endPos;

            if (pageInfo.datas.get() == null) {
                lineList = new ArrayList<>();
                int maxLines = getMaxLines();
                int drawTextWidth = getDrawTextWidth();
                int read = -1;
                String line = "";
                long curPos = startPos;
                while (true) {
                    if (curPos > endPos) {
                        curPos = endPos;
                        break;
                    }
                    read = BookUtils.get(curPos);
                    if (read == -1) {
                        break;
                    } else {
                        char c = (char) read;
                        if (c == '\n') {//换行符，新增一行
                            lineList.add("" + line);
                            line = "";
                        } else {
                            String preLine = line + c;
                            if (mTextPaint.measureText(preLine) > drawTextWidth) {//新增字符后超过绘制宽度，新增一行
                                lineList.add("" + line);
                                line = "" + c;
                            } else {
                                line = line + c;
                            }
                        }
                        if (lineList.size() >= maxLines) {
                            break;
                        }
                        curPos++;
                        if (BookUtils.isChapterFirstPos(curPos)) {
                            curPos--;
                            break;
                        }
                    }
                }

                if (lineList.size() < maxLines) {//当前行数未达到最大值，说明数据读到结尾了
                    if (!line.isEmpty()) {//最后一行存在
                        lineList.add(line);
                    }
                } else if (lineList.size() == maxLines) {//当前行数达到最大值
                    if (!line.isEmpty()) {//最后一行存在
                        curPos -= line.toCharArray().length;
                    }
                }
            } else {
                lineList = pageInfo.datas.get();
            }

            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(PageConfig.pageColor);
            float firstLineBaseLine = getFirstLineBaseLine();
            for (int i = 0; i < lineList.size(); i++) {
                canvas.drawText(lineList.get(i), PageConfig.pagePaddingLeft, firstLineBaseLine + getLineHeight() * i, mTextPaint);
            }

        } catch (Exception e) {
            e.printStackTrace();
            mListener.onError(0);
        }

    }

    /**
     * 加载某章数据
     *
     * @param position
     */
    public static void loadChapter(long position) {
        BookUtils.Chapter curChapter = BookUtils.getChapterByPosition(position);
        if (curChapter == null)
            return;
        BookUtils.Chapter preChapter = BookUtils.getChapterByPosition(curChapter.startPos - 1);
        BookUtils.Chapter nextChapter = BookUtils.getChapterByPosition(curChapter.endPos + 1);
        ArrayList<BookUtils.Chapter> chapterArrayList = new ArrayList<>();

        if (preChapter != null && getPageInfo(preChapter.startPos) == null)
            chapterArrayList.add(preChapter);

        if (getPageInfo(curChapter.startPos) == null)
            chapterArrayList.add(curChapter);


        if (nextChapter != null && getPageInfo(nextChapter.startPos) == null)
            chapterArrayList.add(nextChapter);

        for (int i = 0; i < chapterArrayList.size(); i++) {
            BookUtils.Chapter chapter = chapterArrayList.get(i);
            long startPos = chapter.startPos;
            long endPos = chapter.endPos;
            while (true) {
                PageInfo pageInfo = calculationPage(startPos, endPos);
                if (pageInfo == null) {
                    break;
                } else {
                    insertPageInfo(pageInfo);
                    if (pageInfo.endPos == chapter.endPos) {
                        break;
                    } else {
                        startPos = pageInfo.endPos + 1;
                    }
                }
            }
        }
    }


    private static PageInfo calculationPage(long startPos, long endPos) {
        ArrayList<String> lineList = new ArrayList<>();
        try {
            int maxLines = getMaxLines();
            int drawTextWidth = getDrawTextWidth();
            int read = -1;
            String line = "";
            long curPos = startPos;
            while (true) {
                if (curPos > endPos) {
                    curPos = endPos;
                    break;
                }
                read = BookUtils.get(curPos);
                if (read == -1) {
                    break;
                } else {
                    char c = (char) read;
                    if (c == '\n') {//换行符，新增一行
                        lineList.add("" + line);
                        line = "";
                    } else {
                        String preLine = line + c;
                        if (mTextPaint.measureText(preLine) > drawTextWidth) {//新增字符后超过绘制宽度，新增一行
                            lineList.add("" + line);
                            line = "" + c;
                        } else {
                            line = line + c;
                        }
                    }
                    if (lineList.size() >= maxLines) {
                        break;
                    }
                    curPos++;
                    if (BookUtils.isChapterFirstPos(curPos)) {
                        curPos--;
                        break;
                    }
                }
            }

            if (lineList.size() < maxLines) {//当前行数未达到最大值，说明数据读到结尾了
                if (!line.isEmpty()) {//最后一行存在
                    lineList.add(line);
                }
            } else if (lineList.size() == maxLines) {//当前行数达到最大值
                if (!line.isEmpty()) {//最后一行存在
                    curPos -= line.toCharArray().length;
                }
            }

            boolean show = false;
            if (!lineList.isEmpty()) {
                for (String str : lineList) {
                    if (!TextUtils.isEmpty(str)) {
                        show = true;
                        break;
                    }
                }
            }

            if (!show){
                lineList.clear();
                lineList.add("本章完");
            }

            return new PageInfo(show, startPos, curPos, new WeakReference<>(lineList));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static float getFirstLineBaseLine() {
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        return -fontMetrics.ascent + PageConfig.pagePaddingTop;
    }


    //字体行高
    public static float getLineHeight() {
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        return fontMetrics.descent - fontMetrics.ascent + PageConfig.lineSpace;
    }

    //最大行数
    public static int getMaxLines() {
        return (int) (getDrawTextHeight() / getLineHeight());
    }

    public static int getDrawTextWidth() {
        return PageConfig.pageWidth - PageConfig.pagePaddingLeft - PageConfig.pagePaddingRight;
    }

    public static int getDrawTextHeight() {
        return PageConfig.pageHeight - PageConfig.pagePaddingTop - PageConfig.pagePaddingBottom;
    }


    public interface Listener {

        void onCacheBookSuccess();

        void onCacheBookFail();

        void onMeasureFinish();

        void onProgress(int progress);

        void onError(int what);

        void onShowOrHideOptionLayout(int type);


    }


    static class PageInfo {
        public boolean show = false;
        public long startPos = Long.MAX_VALUE;
        public long endPos = Long.MAX_VALUE;
        public WeakReference<List<String>> datas;


        public PageInfo() {
        }

        public PageInfo(boolean show, long startPos, long endPos, WeakReference<List<String>> datas) {
            this.show = show;
            this.startPos = startPos;
            this.endPos = endPos;
            this.datas = datas;
        }
    }

}

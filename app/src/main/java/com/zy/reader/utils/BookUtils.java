package com.zy.reader.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class BookUtils {
    private static String cacheBookDir;
    private static String bookPath;
    private static final int MAX_CACHE_SIZE = 30000;
    private static OnCacheListener onCacheListener;

    public static ArrayList<Chapter> chapterList = new ArrayList<>();
    public static long bookLength = 0L;

    public static synchronized void cacheBook(String cacheBookDir, String bookPath, OnCacheListener onCacheListener) {
        BookUtils.cacheBookDir = cacheBookDir;
        BookUtils.bookPath = bookPath;
        BookUtils.onCacheListener = onCacheListener;
        cleanAndCreateDir();

        chapterList = new ArrayList<>();

        generateChapterList();

        BookUtils.onCacheListener.onSuccess();
    }

    private static void cleanAndCreateDir() {
        File dir = new File(cacheBookDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                f.delete();
            }
        }
    }


    /**
     * 获取某个字节
     *
     * @param position
     * @return
     */
    public static int get(long position) {
        int result = -1;

        int index = getChapterIndexByPosition(position);

        if (index == -1)
            return result;

        char[] dataArray = getChapterData(index);

        if (dataArray == null)
            return result;

        Chapter chapter = getChapter(index);

        if (chapter == null)
            return result;

        int offset = (int) (position - chapter.startPos);

        if (dataArray != null && offset >= 0 && offset < dataArray.length) {
            result = dataArray[offset];
        }

        return result;
    }


    public static boolean isChapterFirstPos(long position) {

        int left = 0;
        int right = chapterList.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (position == chapterList.get(mid).startPos) {
                return true;
            } else if (chapterList.get(mid).startPos < position) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return false;
    }


    public static boolean isChapterEndPos(long position) {
        int left = 0;
        int right = chapterList.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (position == chapterList.get(mid).endPos) {
                return true;
            } else if (chapterList.get(mid).endPos < position) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return false;
    }

    /**
     * 根据position获取章节索引
     *
     * @param position
     * @return
     */
    public static Chapter getChapterByPosition(long position) {
        if (position < 0 || position > bookLength - 1) {
            return null;
        }
        int left = 0;
        int right = chapterList.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (position >= chapterList.get(mid).startPos && position <= chapterList.get(mid).endPos) {
                return chapterList.get(mid);
            } else if (chapterList.get(mid).endPos < position) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return null;
    }


    public static int getChapterIndexByPosition(long position) {
        if (position < 0 || position > bookLength - 1) {
            return -1;
        }
        int left = 0;
        int right = chapterList.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (position >= chapterList.get(mid).startPos && position <= chapterList.get(mid).endPos) {
                return mid;
            } else if (chapterList.get(mid).endPos < position) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return -1;
    }

    public static Chapter getChapter(int index) {
        if (index >= 0 && index < chapterList.size()) {
            return chapterList.get(index);
        }
        return null;
    }


    /**
     * 获取索引是index的字节数组
     *
     * @param index
     * @return
     */
    private static char[] getChapterData(int index) {
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        try {

            Chapter chapter = chapterList.get(index);
            char[] datas = chapter.datas == null ? null : chapter.datas.get();
            if (datas == null) {
                if (TextUtils.isEmpty(chapter.cacheFile)) {
                    chapter.cacheFile = new File(cacheBookDir, System.currentTimeMillis() + ".txt").getPath();
                }
                File file = new File(chapter.cacheFile);
                if (file.exists() || file.length() > 0) {
                    fileInputStream = new FileInputStream(file);
                    inputStreamReader = new InputStreamReader(fileInputStream, "UTF-16LE");
                    int length = (int) (chapter.endPos - chapter.startPos + 1);
                    length = Math.max(length, 0);
                    datas = new char[length];
                    inputStreamReader.read(datas);
                    chapter.datas = new WeakReference<>(datas);
                } else {
                    fileInputStream = new FileInputStream(bookPath);
                    String charset = FileUtils.getCharset(bookPath);
                    charset=TextUtils.isEmpty(charset)?"UTF-8":charset;

                    inputStreamReader = new InputStreamReader(fileInputStream, charset);
                    inputStreamReader.skip(Math.max(0, chapter.startPos));

                    int length = (int) (chapter.endPos - chapter.startPos + 1);
                    length = Math.max(length, 0);
                    datas = new char[length];
                    inputStreamReader.read(datas);
                    chapter.datas = new WeakReference<>(datas);

                    writeDataToChapterFile(chapter, datas);

                }
            }
            return datas;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {

                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {

                }
            }
        }
        return null;
    }

    private static void writeDataToChapterFile(Chapter chapter, char[] datas) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(chapter.cacheFile), "UTF-16LE");

                    outputStreamWriter.write(datas);

                    outputStreamWriter.flush();

                    outputStreamWriter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private static  boolean generateChapterList() {
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        long curTime = System.currentTimeMillis();
        try {
            long size = 0;
            Chapter preChapter = new Chapter();
            preChapter.name = "序章";
            preChapter.startPos = 0;
            chapterList.add(preChapter);
            StringBuilder lineSb = new StringBuilder();
            String charset = FileUtils.getCharset(bookPath);
            if (TextUtils.isEmpty(charset)) {
                charset = "UTF-8";
            }
            int read = -1;
            fileInputStream = new FileInputStream(bookPath);
            inputStreamReader = new InputStreamReader(fileInputStream, charset);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            while (true) {
                read = inputStreamReader.read();
                if (read == -1) {
                    break;
                }
                char cr = (char) read;
                size++;
                long curPos = size - 1;
                if (cr == '\n') {//碰到换行符
                    String str = lineSb.toString();
                    if (str.matches(".*第.{1,8}章.*") || str.matches(".*第.{1,8}节.*")) {
                        preChapter.endPos = Math.max(0, curPos - str.toCharArray().length - 1);
                        preChapter = new Chapter();
                        preChapter.name = str;
                        preChapter.startPos = curPos - str.toCharArray().length;
                        chapterList.add(preChapter);
                    }
                    lineSb = new StringBuilder();
                } else {
                    lineSb.append(cr);
                    if ((curPos - preChapter.startPos + 1) >= MAX_CACHE_SIZE) {
                        preChapter.endPos = Math.max(0, curPos);
                        preChapter = new Chapter();
                        preChapter.name = "系统分章";
                        preChapter.startPos = curPos + 1;
                        chapterList.add(preChapter);
                        lineSb = new StringBuilder();
                    }
                }
            }
            bookLength = size;
            if (!chapterList.isEmpty()) {
                Chapter lastChapter = chapterList.get(chapterList.size() - 1);
                lastChapter.endPos = size - 1;
            }
            Log.d("TTTT", "花费时间：" + (System.currentTimeMillis() - curTime) / 1000);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }


    public static List<Chapter> getChapterList() {
        return chapterList;
    }


    public static class Chapter {

        public String name;

        public long startPos;

        public long endPos;

        public String cacheFile;

        public WeakReference<char[]> datas;

        public Chapter() {
        }

        @Override
        public String toString() {
            return "Chapter{" +
                    "name='" + name + '\'' +
                    ", startPos=" + startPos +
                    ", endPos=" + endPos +
                    ", cacheFile='" + cacheFile + '\'' +
                    ", datas=" + datas +
                    '}';
        }
    }


    static class CacheFile {
        public String path;
        public long startPos;
        public long endPos;

        public WeakReference<char[]> data;

    }


    public static interface OnCacheListener {
        void onSuccess();

        void onError();

    }
}

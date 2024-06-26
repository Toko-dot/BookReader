package com.zy.reader.utils;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    public static ArrayList<CacheFile> cacheFileList = new ArrayList<>();
    public static ArrayList<Chapter> chapterList = new ArrayList<>();
    public static long bookLength = 0L;

    public static synchronized void cacheBook(String cacheBookDir, String bookPath, OnCacheListener onCacheListener) {
        BookUtils.cacheBookDir = cacheBookDir;
        BookUtils.bookPath = bookPath;
        BookUtils.onCacheListener = onCacheListener;
        cleanAndCreateDir();
        cacheFileList = new ArrayList<>();
        cacheBook();
        getChapterList();
        onCacheListener.onSuccess();
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

    private static void cacheBook() {
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        try {

            String charset = FileUtils.getCharset(bookPath);
            if (TextUtils.isEmpty(charset)) {
                charset = "UTF-8";
            }

            int index = 0;
            long total = 0;
            fileInputStream = new FileInputStream(bookPath);
            inputStreamReader = new InputStreamReader(fileInputStream, charset);
            while (true) {
                index++;

                File file = new File(cacheBookDir, index + ".txt");
                char[] buffArray = new char[MAX_CACHE_SIZE];
                int result = inputStreamReader.read(buffArray);

                if (result == -1) {//没有数据
                    file.delete();
                    break;
                }

                CacheFile cacheFile = new CacheFile();
                cacheFile.path = file.getPath();
                cacheFile.startPos = total;

                String bufStr = new String(buffArray);

                bufStr=bufStr.replaceAll("\u0000", "");//\u0000 在侧量时不计算，替换以下

                buffArray = bufStr.toCharArray();

                total += buffArray.length;

                cacheFile.endPos = (total - 1) < 0 ? 0 : (total - 1);
                cacheFile.data = new WeakReference<char[]>(buffArray);

                cacheFileList.add(index - 1, cacheFile);

                outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-16LE");
                outputStreamWriter.write(buffArray);
                outputStreamWriter.flush();//刷新一下，不然有时会没数据
                outputStreamWriter.close();

                if (result < MAX_CACHE_SIZE) {//读到结尾了
                    break;
                }
            }
            bookLength = total;

        } catch (Exception e) {
            e.printStackTrace();
            onCacheListener.onError();
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

            if (outputStreamWriter != null) {
                try {
                    outputStreamWriter.close();
                } catch (IOException e) {

                }
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
        int index = (int) (position / MAX_CACHE_SIZE);
        int offset = (int) (position % MAX_CACHE_SIZE);
        char[] dataArray = getCharArray(index);
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


    /**
     * 获取索引是index的字节数组
     *
     * @param index
     * @return
     */
    private static char[] getCharArray(int index) {
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        try {
            CacheFile cacheFile = cacheFileList.get(index);
            char[] datas = cacheFile.data.get();
            if (datas == null) {
                File file = new File(cacheFile.path);
                fileInputStream = new FileInputStream(file);
                inputStreamReader = new InputStreamReader(fileInputStream, "UTF-16LE");
                int length = (int) (cacheFile.endPos - cacheFile.startPos + 1);
                length = Math.max(length, 0);
                datas = new char[length];
                inputStreamReader.read(datas);
                cacheFile.data = new WeakReference<>(datas);
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


    public static synchronized List<Chapter> getChapterList() {
        if (chapterList.isEmpty()) {
            long size = 0;
            chapterList.add(new Chapter("序章", 0));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cacheFileList.size(); i++) {
                char[] charArray = getCharArray(i);
                if (charArray == null)
                    break;
                for (char cr : charArray) {
                    size++;
                    long curPos = size - 1;
                    if (cr == '\n') {//碰到换行符
                        String str = sb.toString();
                        if (str.matches(".*第.{1,8}章\\s+.*") || str.matches(".*第.{1,8}节.*")) {
                            if (!chapterList.isEmpty()) {
                                Chapter lastChapter = chapterList.get(chapterList.size() - 1);
                                lastChapter.endPos = Math.max(0, curPos - str.toCharArray().length - 1);
                            }
                            chapterList.add(new Chapter(str, curPos - str.toCharArray().length));
                        }
                        sb = new StringBuilder();
                    } else {
                        sb.append(cr);
                    }
                }

                if (!chapterList.isEmpty()) {
                    Chapter lastChapter = chapterList.get(chapterList.size() - 1);
                    lastChapter.endPos = bookLength - 1;
                }

            }
            return chapterList;
        } else {
            return chapterList;
        }

    }


    public static class Chapter {

        public String name;

        public long startPos;

        public long endPos;

        public Chapter() {
        }

        public Chapter(String name, long startPos) {
            this.name = name;
            this.startPos = startPos;
        }

        public Chapter(String name, long startPos, long endPos) {
            this.name = name;
            this.startPos = startPos;
            this.endPos = endPos;
        }

        @Override
        public String toString() {
            return "Chapter{" +
                    "name='" + name + '\'' +
                    ", startPos=" + startPos +
                    ", endPos=" + endPos +
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

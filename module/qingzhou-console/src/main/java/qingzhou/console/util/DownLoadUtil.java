package qingzhou.console.util;

import qingzhou.console.impl.ConsoleWarHelper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DownLoadUtil {
    public static final String DOWNLOAD_KEY = "DOWNLOAD_KEY";
    public static final String DOWNLOAD_BLOCK = "DOWNLOAD_BLOCK";
    public static final String DOWNLOAD_OFFSET = "DOWNLOAD_OFFSET";

    private static File downloadCacheDir;
    private static final int DOWNLOAD_BLOCK_SIZE = Integer.parseInt(System.getProperty("qingzhou.DOWNLOAD_BLOCK_SIZE", String.valueOf(1024 * 1024 * 2)));

    private DownLoadUtil() {
    }

    private static void clearHistoricalKey(SimpleDateFormat format, String keySP) {
        File cacheDir = getDownloadCache();
        String[] list = cacheDir.list();
        if (list != null) {
            for (String historicalKey : list) {
                boolean delete = true;
                try {
                    int i = historicalKey.indexOf(keySP);
                    long time = format.parse(historicalKey.substring(0, i)).getTime();
                    if (System.currentTimeMillis() - time < 1000 * 60 * 60 * 24) { // 保留下载失败的文件 key 最多一天，之后清理
                        delete = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (delete) {
                    File temp = FileUtil.newFile(cacheDir, historicalKey);
                    FileUtil.forceDeleteQuietly(temp);
                }
            }
        }
    }

    public static String buildDownloadKey(File[] downloadFiles) throws IOException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String keySP = "-";
        clearHistoricalKey(format, keySP);

        String key = format.format(new Date()) + keySP + UUID.randomUUID().toString().replace("-", "");
        File cacheDir = getDownloadCache();
        File zipTo = FileUtil.newFile(cacheDir, key);
        File tempDir = FileUtil.newFile(cacheDir, UUID.randomUUID().toString());// 保障压缩文件的层次结构
        for (File file : downloadFiles) {
            if (file.exists()) {
                File copyTo = new File(tempDir, file.getName());
                if (file.isDirectory()) {
                    FileUtil.mkdirs(copyTo);
                }
                FileUtil.copyFileOrDirectory(file, copyTo);
            }
        }
        FileUtil.zipFiles(tempDir, zipTo, false);
        FileUtil.forceDeleteQuietly(tempDir);
        return key;
    }

    public static Map<String, Object> downloadFile(String key, long offset) throws Exception {
        if (offset < 0) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();

        synchronized (DownLoadUtil.class) { // 防止并发下载被其它线程删除临时文件
            File cacheDir = getDownloadCache();
            if (StringUtil.isBlank(key) || !FileUtil.newFile(cacheDir, key).exists()) {
                return null;
            }

            File downloadFile = FileUtil.newFile(cacheDir, key);
            result.put(DOWNLOAD_KEY, key);

            byte[] byteRead = null;
            try (RandomAccessFile raf = new RandomAccessFile(downloadFile, "r")) {
                raf.seek(offset);
                byte[] block = new byte[DOWNLOAD_BLOCK_SIZE];
                int read = raf.read(block);
                if (read > 0) { // ==0 表示上次正好读取到结尾
                    if (read == block.length) {
                        byteRead = block;
                    } else {
                        byteRead = new byte[read];
                        System.arraycopy(block, 0, byteRead, 0, read);
                    }
                    result.put(DOWNLOAD_BLOCK, byteRead);
                    offset = raf.getFilePointer();
                }
            }

            if (byteRead != null && byteRead.length == DOWNLOAD_BLOCK_SIZE) {
                result.put(DOWNLOAD_OFFSET, offset);
            } else {
                result.put(DOWNLOAD_OFFSET, -1L);
                File temp = FileUtil.newFile(cacheDir, key);
                FileUtil.forceDeleteQuietly(temp);
            }
        }

        return result;
    }

    private static File getDownloadCache() {
        if (downloadCacheDir == null) {
            downloadCacheDir = FileUtil.newFile(ConsoleWarHelper.getCache(), "download");
        }
        FileUtil.mkdirs(downloadCacheDir);

        return downloadCacheDir;
    }
}

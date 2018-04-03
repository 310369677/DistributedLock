package com.yang.lock.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 描述:
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-30
 */
public class SimpleUtils {

    private static final int TRY_MAX_COUNT = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUtils.class);

    public static byte[] getBytes(long data) {

        byte[] bytes = new byte[8];


        bytes[0] = (byte) (data & 0xff);


        bytes[1] = (byte) ((data >> 8) & 0xff);


        bytes[2] = (byte) ((data >> 16) & 0xff);


        bytes[3] = (byte) ((data >> 24) & 0xff);


        bytes[4] = (byte) ((data >> 32) & 0xff);


        bytes[5] = (byte) ((data >> 40) & 0xff);


        bytes[6] = (byte) ((data >> 48) & 0xff);


        bytes[7] = (byte) ((data >> 56) & 0xff);


        return bytes;
    }

    public static long getLong(byte[] bytes)


    {


        return (0xffL & (long) bytes[0]) | (0xff00L & ((long) bytes[1] << 8)) | (0xff0000L & ((long) bytes[2] << 16)) | (0xff000000L & ((long) bytes[3] << 24))


                | (0xff00000000L & ((long) bytes[4] << 32)) | (0xff0000000000L & ((long) bytes[5] << 40)) | (0xff000000000000L & ((long) bytes[6] << 48)) | (0xff00000000000000L & ((long) bytes[7] << 56));


    }

    public static <T> T asMuchasPossibleRetry(Run<T> r) {
        int count = 0;
        while (count < TRY_MAX_COUNT) {
            try {
                return r.run();
            } catch (Exception e) {
                LOGGER.warn("网络闪断", e);
            }
            count++;
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                LOGGER.warn("当前线程被打断", e);
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    public interface Run<T> {
        T run() throws Exception;
    }

    /**
     * 文件路劲组合
     *
     * @param paths 需要组装的路径
     * @return 组装后的结果
     */
    public static String pathJoin(String... paths) {
        String windowSeparate = "\\";
        String commonSeparate = "/";
        if (paths == null || paths.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            //忽略空字符串
            if (path == null || "".equals(path)) {
                continue;
            }
            path = path.replace(windowSeparate, commonSeparate);
            path = path.replaceAll("/+", commonSeparate);

            //去掉头部的"/"
            if (path.startsWith(commonSeparate) && (i > 0)) {
                path = path.substring(1, path.length());
            }
            //去掉尾部的"/"
            if (path.endsWith(commonSeparate) && (i < paths.length - 1)) {
                path = path.substring(0, path.length() - 1);
            }
            builder.append(path);
            //非最后的尾部就添加"/"
            if (i < paths.length - 1) {
                builder.append(commonSeparate);
            }
        }
        return builder.toString();
    }
}

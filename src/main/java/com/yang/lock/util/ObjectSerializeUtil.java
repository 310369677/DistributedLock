package com.yang.lock.util;

import java.io.*;

/**
 * 描述:对象序列化工具
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-4-2
 */
public class ObjectSerializeUtil {

    private ObjectSerializeUtil() {

    }

    public static byte[] writeObj2Byte(Serializable serializable) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(serializable);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    System.err.println("关闭流失败:" + e.getMessage());
                }
            }
        }
    }

    public static <T> T readObjFromByte(byte[] bytes) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (T) in.readObject();
        } catch (Exception e) {
            System.err.println("反序列化失败:" + e.getMessage());
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    System.err.println("关闭流失败:" + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {

    }
}

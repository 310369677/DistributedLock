package com.yang.lock.exceptions;

/**
 * 描述:
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-4-2
 */
public class LockInValiteException extends RuntimeException {

    public LockInValiteException() {
    }

    public LockInValiteException(String message) {
        super(message);
    }

    public LockInValiteException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockInValiteException(Throwable cause) {
        super(cause);
    }

    public LockInValiteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

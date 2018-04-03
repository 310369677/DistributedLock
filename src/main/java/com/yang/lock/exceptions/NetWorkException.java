package com.yang.lock.exceptions;

/**
 * 描述:
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-23
 */
public class NetWorkException extends RuntimeException {

    public NetWorkException() {
    }

    public NetWorkException(String message) {
        super(message);
    }

    public NetWorkException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetWorkException(Throwable cause) {
        super(cause);
    }

    public NetWorkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

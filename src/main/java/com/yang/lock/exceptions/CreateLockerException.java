package com.yang.lock.exceptions;

/**
 * 描述:创建锁异常
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-30
 */
public class CreateLockerException extends RuntimeException {

    public CreateLockerException() {
    }

    public CreateLockerException(String message) {
        super(message);
    }

    public CreateLockerException(String message, Throwable cause) {
        super(message, cause);
    }

    public CreateLockerException(Throwable cause) {
        super(cause);
    }

    public CreateLockerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

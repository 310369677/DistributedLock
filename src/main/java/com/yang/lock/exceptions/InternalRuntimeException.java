package com.yang.lock.exceptions;

/**
 * 描述:
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-23
 */
public class InternalRuntimeException extends RuntimeException {

    public InternalRuntimeException() {
    }

    public InternalRuntimeException(String message) {
        super(message);
    }

    public InternalRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalRuntimeException(Throwable cause) {
        super(cause);
    }

    public InternalRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

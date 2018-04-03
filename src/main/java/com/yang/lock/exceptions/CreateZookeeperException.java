package com.yang.lock.exceptions;

/**
 * 描述:
 * 公司:jwell
 * 作者:杨川东
 * 日期:18-3-30
 */
public class CreateZookeeperException extends RuntimeException {
    public CreateZookeeperException() {
    }

    public CreateZookeeperException(String message) {
        super(message);
    }

    public CreateZookeeperException(String message, Throwable cause) {
        super(message, cause);
    }

    public CreateZookeeperException(Throwable cause) {
        super(cause);
    }

    public CreateZookeeperException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

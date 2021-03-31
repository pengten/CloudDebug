package com.myagent.web;

public class AttachException extends RuntimeException {

    public AttachException() {
        super();
    }

    public AttachException(String message) {
        super(message);
    }

    public AttachException(String message, Throwable cause) {
        super(message, cause);
    }

    public AttachException(Throwable cause) {
        super(cause);
    }

    protected AttachException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

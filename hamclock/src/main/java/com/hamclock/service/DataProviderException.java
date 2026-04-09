package com.hamclock.service;

/**
 * Typed exception thrown by data providers on unrecoverable failures.
 */
public class DataProviderException extends Exception {

    public enum ErrorCode {
        NETWORK_ERROR,
        PARSE_ERROR,
        AUTH_ERROR,
        TIMEOUT,
        NOT_CONFIGURED,
        UNKNOWN
    }

    private final ErrorCode errorCode;

    public DataProviderException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public DataProviderException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

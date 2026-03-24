package com.f1predict.f1data.client;

public class F1ApiException extends RuntimeException {
    public F1ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

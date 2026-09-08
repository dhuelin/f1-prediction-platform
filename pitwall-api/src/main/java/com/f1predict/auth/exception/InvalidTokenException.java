package com.f1predict.auth.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() { super("Invalid or expired token"); }
}

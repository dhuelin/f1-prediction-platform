package com.f1predict.auth.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("Username already in use: " + username);
    }
}

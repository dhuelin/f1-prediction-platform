package com.f1predict.auth.controller;

import com.f1predict.auth.exception.EmailAlreadyExistsException;
import com.f1predict.auth.exception.UsernameAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({EmailAlreadyExistsException.class, UsernameAlreadyExistsException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleConflict() {}
}

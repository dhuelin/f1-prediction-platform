package com.f1predict.prediction.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * Scoped to the prediction domain's controllers.
 *
 * Was one of three classes all named GlobalExceptionHandler — component scanning
 * derives the bean name from the simple class name, so sharing a context made
 * startup fail with a ConflictingBeanDefinitionException. basePackages keeps each
 * advice applying only to its own controllers, exactly as it did when these were
 * separate services; without it every domain would silently inherit the others'
 * exception mappings.
 */
@RestControllerAdvice(basePackages = "com.f1predict.prediction.controller")
public class PredictionExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleConflict() {}

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound() {}

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public void handleInvalidArgument() {}

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleConstraintViolation() {}
}

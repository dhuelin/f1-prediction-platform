package com.f1predict.league.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.NoSuchElementException;

/**
 * Scoped to the league domain's controllers.
 *
 * Was one of three classes all named GlobalExceptionHandler — component scanning
 * derives the bean name from the simple class name, so sharing a context made
 * startup fail with a ConflictingBeanDefinitionException. basePackages keeps each
 * advice applying only to its own controllers, exactly as it did when these were
 * separate services; without it every domain would silently inherit the others'
 * exception mappings.
 */
@RestControllerAdvice(basePackages = "com.f1predict.league.controller")
public class LeagueExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleConflict() {}

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound() {}

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public void handleInvalidArgument() {}

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void handleForbidden() {}
}

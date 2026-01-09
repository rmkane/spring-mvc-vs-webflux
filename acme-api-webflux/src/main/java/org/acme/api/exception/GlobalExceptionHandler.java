package org.acme.api.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthorizationDeniedException.class)
    public Mono<ProblemDetail> handleAuthorizationDenied(
            AuthorizationDeniedException ex, ServerWebExchange exchange) {
        log.warn("Authorization denied: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Authorization Denied");
        problemDetail.setProperty("error", "Forbidden");
        problemDetail.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(BookNotFoundException.class)
    public Mono<ProblemDetail> handleBookNotFound(
            BookNotFoundException ex, ServerWebExchange exchange) {
        log.warn("Book not found: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Book Not Found");
        problemDetail.setProperty("error", "Not Found");
        problemDetail.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(BookAlreadyExistsException.class)
    public Mono<ProblemDetail> handleBookAlreadyExists(
            BookAlreadyExistsException ex, ServerWebExchange exchange) {
        log.warn("Book already exists: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Book Already Exists");
        problemDetail.setProperty("error", "Bad Request");
        problemDetail.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ProblemDetail> handleValidationException(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Validation failed");
        log.warn("Validation error: {}", message);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, message);
        problemDetail.setTitle("Validation Failed");
        problemDetail.setProperty("error", "Bad Request");
        problemDetail.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(RuntimeException.class)
    public Mono<ProblemDetail> handleRuntimeException(
            RuntimeException ex, ServerWebExchange exchange) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("error", "Internal Server Error");
        problemDetail.setInstance(URI.create(exchange.getRequest().getPath().value()));
        return Mono.just(problemDetail);
    }
}

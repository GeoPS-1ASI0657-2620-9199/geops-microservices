package com.geopslabs.geops.identity.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.ServletException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INVALID_REQUEST = "INVALID_REQUEST";
    private static final String INVALID_REQUEST_MESSAGE = "Revisa los datos enviados.";
    private static final String INVALID_FIELDS_MESSAGE = "Revisa estos datos: %s.";
    private static final String FIELD_SEPARATOR = ", ";
    private static final String REQUEST_NOT_SUPPORTED = "REQUEST_NOT_SUPPORTED";
    private static final String REQUEST_NOT_SUPPORTED_MESSAGE = "Esa operación no está disponible.";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    private static final String INTERNAL_ERROR_MESSAGE = "No pudimos completar la operación. Inténtalo de nuevo.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArguments(MethodArgumentNotValidException exception) {
        var fields = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .sorted()
                .collect(Collectors.joining(FIELD_SEPARATOR));
        LOGGER.info("request.rejected code={} fields={}", INVALID_REQUEST, fields);
        return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST, INVALID_FIELDS_MESSAGE.formatted(fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        LOGGER.info("request.rejected code={} reason=unreadable-body", INVALID_REQUEST);
        return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST, INVALID_REQUEST_MESSAGE);
    }

    @ExceptionHandler(RuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleRuleViolation(RuleViolationException exception) {
        return respondTo(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
        return respondTo(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticated(UnauthenticatedException exception) {
        return respondTo(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedRequest(ServletException exception) {
        var status = exception instanceof org.springframework.web.ErrorResponse framework
                ? framework.getStatusCode() : HttpStatus.BAD_REQUEST;
        LOGGER.info("request.rejected code={} status={}", REQUEST_NOT_SUPPORTED, status.value());
        return respond(status, REQUEST_NOT_SUPPORTED, REQUEST_NOT_SUPPORTED_MESSAGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        LOGGER.error("request.failed code={}", INTERNAL_ERROR, exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, INTERNAL_ERROR_MESSAGE);
    }

    private ResponseEntity<ErrorResponse> respondTo(HttpStatus status, DomainException exception) {
        LOGGER.info("request.rejected code={}", exception.getCode());
        return respond(status, exception.getCode(), exception.getMessage());
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatusCode status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}

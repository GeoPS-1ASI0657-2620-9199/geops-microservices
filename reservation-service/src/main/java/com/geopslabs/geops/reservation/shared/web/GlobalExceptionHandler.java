package com.geopslabs.geops.reservation.shared.web;

import com.geopslabs.geops.reservation.shared.domain.ConflictException;
import com.geopslabs.geops.reservation.shared.domain.DomainException;
import com.geopslabs.geops.reservation.shared.domain.ForbiddenException;
import com.geopslabs.geops.reservation.shared.domain.NotFoundException;
import com.geopslabs.geops.reservation.shared.domain.UnavailableException;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INVALID_REQUEST = "INVALID_REQUEST";
    private static final String INVALID_FIELD_MESSAGE = "%s %s";
    private static final String INVALID_PARAMETER_MESSAGE = "%s has an invalid value";
    private static final String MISSING_PARAMETER_MESSAGE = "%s is required";
    private static final String UNREADABLE_BODY_MESSAGE = "The request body could not be read";
    private static final String MESSAGE_SEPARATOR = "; ";
    private static final String REQUEST_NOT_SUPPORTED = "REQUEST_NOT_SUPPORTED";
    private static final String REQUEST_NOT_SUPPORTED_MESSAGE = "This operation is not available";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    private static final String INTERNAL_ERROR_MESSAGE = "The operation could not be completed. Try again later";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        return respondTo(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
        return respondTo(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exception) {
        return respondTo(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(UnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(UnavailableException exception) {
        return respondTo(HttpStatus.SERVICE_UNAVAILABLE, exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException exception) {
        var message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> INVALID_FIELD_MESSAGE.formatted(error.getField(), error.getDefaultMessage()))
                .sorted()
                .collect(Collectors.joining(MESSAGE_SEPARATOR));
        var fields = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .collect(Collectors.joining(MESSAGE_SEPARATOR));
        LOGGER.info("request.rejected code={} fields={}", INVALID_REQUEST, fields);
        return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        LOGGER.info("request.rejected code={} parameter={}", INVALID_REQUEST, exception.getName());
        return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST,
                INVALID_PARAMETER_MESSAGE.formatted(exception.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception) {
        LOGGER.info("request.rejected code={} parameter={}", INVALID_REQUEST, exception.getParameterName());
        return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST,
                MISSING_PARAMETER_MESSAGE.formatted(exception.getParameterName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        LOGGER.info("request.rejected code={} reason=unreadable-body", INVALID_REQUEST);
        return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST, UNREADABLE_BODY_MESSAGE);
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

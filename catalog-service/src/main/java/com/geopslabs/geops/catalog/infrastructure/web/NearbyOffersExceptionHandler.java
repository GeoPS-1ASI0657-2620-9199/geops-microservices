package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.services.InvalidPageRequestException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidGeoPointException;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidSearchRadiusException;
import com.geopslabs.geops.catalog.shared.domain.DomainException;
import com.geopslabs.geops.catalog.shared.web.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = NearbyOffersController.class)
public class NearbyOffersExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NearbyOffersExceptionHandler.class);
    private static final String MISSING_PARAMETER = "MISSING_PARAMETER";
    private static final String MISSING_PARAMETER_MESSAGE = "Falta el parámetro %s";
    private static final String INVALID_PARAMETER = "INVALID_PARAMETER";
    private static final String INVALID_PARAMETER_MESSAGE = "El parámetro %s no tiene un valor válido";

    @ExceptionHandler({InvalidSearchRadiusException.class, InvalidGeoPointException.class,
            InvalidPageRequestException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleRejectedSearch(DomainException exception) {
        return reject(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissing(MissingServletRequestParameterException exception) {
        return reject(MISSING_PARAMETER, MISSING_PARAMETER_MESSAGE.formatted(exception.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMismatch(MethodArgumentTypeMismatchException exception) {
        return reject(INVALID_PARAMETER, INVALID_PARAMETER_MESSAGE.formatted(exception.getName()));
    }

    private static ErrorResponse reject(String code, String message) {
        LOGGER.info("offers.nearby.rejected code={}", code);
        return new ErrorResponse(code, message);
    }
}

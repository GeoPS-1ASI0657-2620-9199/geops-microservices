package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCategoryException;
import com.geopslabs.geops.catalog.shared.domain.DomainException;
import com.geopslabs.geops.catalog.shared.web.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AdminOffersController.class)
public class AdminExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminExceptionHandler.class);

    @ExceptionHandler(InvalidCategoryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleRejectedBatch(DomainException exception) {
        LOGGER.info("admin.request.rejected code={}", exception.getCode());
        return new ErrorResponse(exception.getCode(), exception.getMessage());
    }
}

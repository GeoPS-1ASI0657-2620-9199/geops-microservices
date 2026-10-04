package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.domain.models.EmailAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.PhoneAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.RucAlreadyRegisteredException;
import com.geopslabs.geops.identity.shared.ConflictException;
import com.geopslabs.geops.identity.shared.ErrorResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UniqueConstraintAdvice {
    private static final Logger LOGGER = LoggerFactory.getLogger(UniqueConstraintAdvice.class);
    private static final String DATA_CONFLICT = "DATA_CONFLICT";
    private static final String DATA_CONFLICT_MESSAGE = "Los datos enviados ya pertenecen a otra cuenta.";
    private static final Map<String, Supplier<ConflictException>> CONFLICTS_BY_CONSTRAINT = Map.of(
            "uk_users_email", EmailAlreadyRegisteredException::new,
            "uk_users_phone", PhoneAlreadyRegisteredException::new,
            "uk_business_profiles_ruc", RucAlreadyRegisteredException::new);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleUniqueViolation(DataIntegrityViolationException exception) {
        var conflict = constraintName(exception)
                .map(CONFLICTS_BY_CONSTRAINT::get)
                .map(Supplier::get);
        var code = conflict.map(ConflictException::getCode).orElse(DATA_CONFLICT);
        var message = conflict.map(ConflictException::getMessage).orElse(DATA_CONFLICT_MESSAGE);
        LOGGER.info("request.rejected code={} reason=unique-constraint", code);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(code, message));
    }

    static Optional<String> constraintName(Throwable exception) {
        for (var cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return Optional.of(violation.getConstraintName().toLowerCase(Locale.ROOT));
            }
        }
        return Optional.empty();
    }
}

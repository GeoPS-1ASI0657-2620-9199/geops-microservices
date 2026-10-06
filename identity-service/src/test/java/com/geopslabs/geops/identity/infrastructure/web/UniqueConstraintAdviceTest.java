package com.geopslabs.geops.identity.infrastructure.web;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class UniqueConstraintAdviceTest {
    private final UniqueConstraintAdvice advice = new UniqueConstraintAdvice();

    @Test
    void mapsTheRucConstraintToItsConflictCode() {
        var response = advice.handleUniqueViolation(violationOf("uk_business_profiles_ruc"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("RUC_ALREADY_REGISTERED");
    }

    @Test
    void mapsTheEmailConstraintToItsConflictCode() {
        var response = advice.handleUniqueViolation(violationOf("UK_USERS_EMAIL"));

        assertThat(response.getBody().code()).isEqualTo("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void answersAGenericConflictForAnUnknownConstraint() {
        var response = advice.handleUniqueViolation(violationOf("uk_unknown"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("DATA_CONFLICT");
    }

    private static DataIntegrityViolationException violationOf(String constraintName) {
        var cause = new ConstraintViolationException("duplicate key", new SQLException("23505"), constraintName);
        return new DataIntegrityViolationException("could not execute statement", cause);
    }
}

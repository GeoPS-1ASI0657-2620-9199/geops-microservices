package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidModerationReasonException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationActionTest {
    private static final Instant REMOVED_AT = Instant.parse("2026-11-22T15:04:05Z");

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "Spam", "         corto     "})
    void aReasonShorterThanTenCharactersIsRejected(String reason) {
        assertThatThrownBy(() -> new ModerationAction(null, 1052L, 1L, reason, REMOVED_AT))
                .isInstanceOf(InvalidModerationReasonException.class)
                .hasMessage("El motivo del retiro debe tener entre 10 y 255 caracteres.");
    }

    @Test
    void aReasonLongerThan255CharactersIsRejected() {
        var reason = "x".repeat(ModerationAction.MAX_REASON_LENGTH + 1);

        assertThatThrownBy(() -> new ModerationAction(null, 1052L, 1L, reason, REMOVED_AT))
                .isInstanceOf(InvalidModerationReasonException.class);
    }

    @Test
    void theReasonIsKeptWithoutSurroundingSpaces() {
        var action = new ModerationAction(null, 1052L, 1L, "\tOferta duplicada del mismo local \n", REMOVED_AT);

        assertThat(action.reason()).isEqualTo("Oferta duplicada del mismo local");
        assertThat(action.withId(31L).id()).isEqualTo(31L);
    }
}

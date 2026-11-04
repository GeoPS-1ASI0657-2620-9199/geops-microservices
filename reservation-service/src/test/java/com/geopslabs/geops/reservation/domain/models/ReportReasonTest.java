package com.geopslabs.geops.reservation.domain.models;

import com.geopslabs.geops.reservation.domain.models.exceptions.InvalidReportReasonException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportReasonTest {

    @Test
    void keepsAReasonOfTenToFiveHundredCharactersWithoutBlanksAtTheEnds() {
        assertThat(new ReportReason("  No me aplicaron el 2x1.  ").value()).isEqualTo("No me aplicaron el 2x1.");
        assertThat(new ReportReason("x".repeat(10)).value()).hasSize(10);
        assertThat(new ReportReason("x".repeat(500)).value()).hasSize(500);
    }

    @Test
    void rejectsAShorterOrLongerOrMissingReasonWithTheMessageOfTheApi() {
        for (var invalid : new String[]{null, "", "   corto   ", "x".repeat(9), "x".repeat(501)}) {
            assertThatThrownBy(() -> new ReportReason(invalid)).isInstanceOf(InvalidReportReasonException.class)
                    .hasMessage("Cuéntanos el motivo en 10 a 500 caracteres.");
        }
    }
}

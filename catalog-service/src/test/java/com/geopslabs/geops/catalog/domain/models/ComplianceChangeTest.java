package com.geopslabs.geops.catalog.domain.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceChangeTest {

    @Test
    void aRedemptionCountsForTheIndex() {
        assertThat(ComplianceChange.forRedemption()).isEqualTo(new ComplianceChange(0, 1, 0));
    }

    @Test
    void anOpenedReportIsOpenUntilTheBusinessAnswers() {
        assertThat(ComplianceChange.forReportOpened()).isEqualTo(new ComplianceChange(1, 0, 0));
        assertThat(ComplianceChange.forReportAnswered()).isEqualTo(new ComplianceChange(-1, 0, 0));
    }

    @Test
    void aReportResolvedWhileOpenStopsBeingOpen() {
        assertThat(ComplianceChange.forReportResolved("OPEN", "CONSUMER_FAVOR")).isEqualTo(new ComplianceChange(-1, 0, 1));
        assertThat(ComplianceChange.forReportResolved("OPEN", "BUSINESS_FAVOR")).isEqualTo(new ComplianceChange(-1, 0, 0));
    }

    @Test
    void aReportResolvedFromReviewWasNoLongerOpen() {
        assertThat(ComplianceChange.forReportResolved("IN_REVIEW", "CONSUMER_FAVOR"))
                .isEqualTo(new ComplianceChange(0, 0, 1));
        assertThat(ComplianceChange.forReportResolved("IN_REVIEW", "BUSINESS_FAVOR"))
                .isEqualTo(new ComplianceChange(0, 0, 0));
    }
}

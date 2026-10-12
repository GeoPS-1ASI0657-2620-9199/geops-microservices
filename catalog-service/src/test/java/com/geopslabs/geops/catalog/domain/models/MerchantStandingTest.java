package com.geopslabs.geops.catalog.domain.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantStandingTest {
    private static final Instant MONDAY = Instant.parse("2026-10-05T15:00:00Z");
    private static final Instant TUESDAY = Instant.parse("2026-10-06T15:00:00Z");

    @Test
    void theSealNeedsAVerifiedRucAndAtLeastNinetyPercentOfCompliance() {
        assertThat(MerchantStanding.sealFor(true, new BigDecimal("90.00"))).isTrue();
        assertThat(MerchantStanding.sealFor(true, new BigDecimal("89.99"))).isFalse();
        assertThat(MerchantStanding.sealFor(false, new BigDecimal("100.00"))).isFalse();
        assertThat(MerchantStanding.sealFor(true, null)).isFalse();
    }

    @Test
    void aVerifiedRucGivesTheSealToAProvisionalStanding() {
        var standing = MerchantStanding.provisional(84L, "Restaurante Don Pepe")
                .applyRucVerification(true, "Restaurante Don Pepe S.A.C.", TUESDAY);

        assertThat(standing.hasVerifiedSeal()).isTrue();
        assertThat(standing.isRucVerified()).isTrue();
        assertThat(standing.getBusinessName()).isEqualTo("Restaurante Don Pepe S.A.C.");
        assertThat(standing.getUpdatedAt()).isEqualTo(TUESDAY);
    }

    @Test
    void losingTheVerificationRemovesTheSealButKeepsTheReports() {
        var standing = new MerchantStanding(84L, "Restaurante Don Pepe", true, 2, new BigDecimal("95.00"), true,
                MONDAY).applyRucVerification(false, null, TUESDAY);

        assertThat(standing.hasVerifiedSeal()).isFalse();
        assertThat(standing.getOpenReports()).isEqualTo(2);
        assertThat(standing.getBusinessName()).isEqualTo("Restaurante Don Pepe");
    }

    @Test
    void aVerifiedRucWithLowComplianceDoesNotGiveTheSeal() {
        var standing = new MerchantStanding(84L, "Restaurante Don Pepe", false, 5, new BigDecimal("80.00"), false,
                MONDAY).applyRucVerification(true, "Restaurante Don Pepe", TUESDAY);

        assertThat(standing.isRucVerified()).isTrue();
        assertThat(standing.hasVerifiedSeal()).isFalse();
    }

    @Test
    void aBusinessWithoutCampaignsStartsFromTheEventWithFullCompliance() {
        var standing = MerchantStanding.fromRucVerification(84L, "Restaurante Don Pepe", true, TUESDAY);

        assertThat(standing.getComplianceIndex()).isEqualByComparingTo("100.00");
        assertThat(standing.hasVerifiedSeal()).isTrue();
    }

    @Test
    void theProvisionalStandingIsOlderThanAnyEvent() {
        var provisional = MerchantStanding.provisional(84L, "Restaurante Don Pepe");

        assertThat(provisional.isNewerThan(MONDAY)).isFalse();
        assertThat(provisional.applyRucVerification(true, null, TUESDAY).isNewerThan(MONDAY)).isTrue();
    }
}

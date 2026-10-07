package com.geopslabs.geops.catalog.domain.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignTest {
    private static final Long BUSINESS_ID = 1L;
    private static final Long OTHER_BUSINESS_ID = 2L;

    @Test
    void belongsToItsBusiness() {
        assertThat(campaign().isOfBusiness(BUSINESS_ID)).isTrue();
    }

    @Test
    void doesNotBelongToAnotherBusiness() {
        assertThat(campaign().isOfBusiness(OTHER_BUSINESS_ID)).isFalse();
    }

    @Test
    void doesNotBelongToATokenWithoutBusiness() {
        assertThat(campaign().isOfBusiness(null)).isFalse();
    }

    private static Campaign campaign() {
        var period = new DateRange(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-31"));
        var zone = new CampaignZone(ZoneType.DISTRICT, null, "La Victoria");
        return new Campaign(14L, BUSINESS_ID, "Almuerzos de octubre", "Menú ejecutivo", period, zone,
                CampaignStatus.ACTIVE, Money.soles(BigDecimal.TEN));
    }
}

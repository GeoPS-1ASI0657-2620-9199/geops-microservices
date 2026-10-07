package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.CampaignAlreadyEndedException;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferValidityOutsideCampaignException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignTest {
    private static final Long BUSINESS_ID = 1L;
    private static final Long OTHER_BUSINESS_ID = 2L;
    private static final LocalDate TODAY = LocalDate.parse("2026-10-08");
    private static final DateRange OCTOBER = new DateRange(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-31"));
    private static final GeoPoint STORE = new GeoPoint(-12.1211, -77.0297);
    private static final int RADIUS_METERS = 800;
    private static final String ADDRESS = "Av. Larco 345, Miraflores";

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

    @Test
    void publishedCampaignStartsActiveForItsBusiness() {
        var campaign = publish(OCTOBER);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(campaign.getBusinessId()).isEqualTo(BUSINESS_ID);
        assertThat(campaign.getId()).isNull();
    }

    @Test
    void campaignEndingTodayCanStillBePublished() {
        var endsToday = new DateRange(LocalDate.parse("2026-10-01"), TODAY);

        assertThat(publish(endsToday).getStatus()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    void campaignWhoseValidityEndedCannotBePublished() {
        var september = new DateRange(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"));

        assertThatThrownBy(() -> publish(september))
                .isInstanceOf(CampaignAlreadyEndedException.class)
                .hasMessage("La vigencia de la campaña terminó el 2026-09-30. Elige una fecha de fin desde hoy.");
    }

    @Test
    void offerIsPublishedForTheCampaignBusinessAtTheStore() {
        var offer = Offer.publishFor(publish(OCTOBER), "2x1 en almuerzos", "Lunes a viernes",
                Money.soles(BigDecimal.TEN), LocalDate.parse("2026-10-15"), "Gastronomía", null, ADDRESS, STORE);

        assertThat(offer.getBusinessId()).isEqualTo(BUSINESS_ID);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PUBLISHED);
        assertThat(offer.getSource()).isEqualTo(OfferSource.AFFILIATED);
        assertThat(offer.getGeocodingStatus()).isEqualTo(GeocodingStatus.GEOCODED);
        assertThat(offer.getAddress()).isEqualTo(ADDRESS);
        assertThat(offer.getLocation()).isEqualTo(STORE);
    }

    @Test
    void offerValidAfterTheCampaignIsRejected() {
        var campaign = publish(OCTOBER);
        var november = LocalDate.parse("2026-11-15");

        assertThatThrownBy(() -> Offer.publishFor(campaign, "Noviembre", "Sin condiciones",
                Money.soles(BigDecimal.TEN), november, "Gastronomía", null, ADDRESS, STORE))
                .isInstanceOf(OfferValidityOutsideCampaignException.class);
    }

    private static Campaign publish(DateRange period) {
        return Campaign.publish(BUSINESS_ID, "Almuerzos de octubre", "Menú ejecutivo", period,
                CampaignZone.radius(STORE, RADIUS_METERS), Money.soles(BigDecimal.TEN), TODAY);
    }

    private static Campaign campaign() {
        var zone = new CampaignZone(ZoneType.DISTRICT, null, null, "La Victoria");
        return new Campaign(14L, BUSINESS_ID, "Almuerzos de octubre", "Menú ejecutivo", OCTOBER, zone,
                CampaignStatus.ACTIVE, Money.soles(BigDecimal.TEN));
    }
}

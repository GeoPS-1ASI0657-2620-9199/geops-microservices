package com.geopslabs.geops.identity.domain.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RucVerificationTest {
    private static final Long PROFILE_ID = 84L;
    private static final Ruc RUC = new Ruc("20123456789");
    private static final Instant NOW = Instant.parse("2026-10-21T15:04:00Z");
    private static final Taxpayer ACTIVE_HABIDO = new Taxpayer(RUC, "RESTAURANTE DON PEPE S.A.C.", "ACTIVO", "HABIDO");
    private static final GeoPoint LARCO = new GeoPoint(-12.1211, -77.0297);

    @Test
    void approvesAnActiveAndHabidoTaxpayerWithAGeocodedAddress() {
        var verification = evaluate(Optional.of(ACTIVE_HABIDO), () -> GeocodingResult.found(LARCO));

        assertThat(verification.result()).isEqualTo(VerificationResult.APPROVED);
        assertThat(verification.reason()).isNull();
        assertThat(verification.taxpayerStatus()).isEqualTo("ACTIVO");
        assertThat(verification.taxpayerCondition()).isEqualTo("HABIDO");
        assertThat(verification.checkedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsARucThatIsNotInTheRegistryWithoutGeocoding() {
        var geocoder = new CountingGeocoder(GeocodingResult.found(LARCO));

        var verification = evaluate(Optional.empty(), geocoder);

        assertThat(verification.result()).isEqualTo(VerificationResult.REJECTED);
        assertThat(verification.reason()).isEqualTo(VerificationReason.RUC_NOT_FOUND);
        assertThat(verification.taxpayerStatus()).isNull();
        assertThat(geocoder.calls()).isZero();
    }

    @Test
    void rejectsATaxpayerThatIsNotActiveAndKeepsTheStatusRead() {
        var geocoder = new CountingGeocoder(GeocodingResult.found(LARCO));
        var closed = new Taxpayer(RUC, "RESTAURANTE DON PEPE S.A.C.", "BAJA DE OFICIO", "HABIDO");

        var verification = evaluate(Optional.of(closed), geocoder);

        assertThat(verification.reason()).isEqualTo(VerificationReason.TAXPAYER_NOT_ACTIVE);
        assertThat(verification.taxpayerStatus()).isEqualTo("BAJA DE OFICIO");
        assertThat(geocoder.calls()).isZero();
    }

    @Test
    void rejectsATaxpayerThatIsNotHabido() {
        var noHabido = new Taxpayer(RUC, "RESTAURANTE DON PEPE S.A.C.", "ACTIVO", "NO HABIDO");

        var verification = evaluate(Optional.of(noHabido), () -> GeocodingResult.found(LARCO));

        assertThat(verification.result()).isEqualTo(VerificationResult.REJECTED);
        assertThat(verification.reason()).isEqualTo(VerificationReason.TAXPAYER_NOT_HABIDO);
        assertThat(verification.taxpayerCondition()).isEqualTo("NO HABIDO");
    }

    @Test
    void rejectsAnAddressTheGeocoderCannotFind() {
        var verification = evaluate(Optional.of(ACTIVE_HABIDO), GeocodingResult::notFound);

        assertThat(verification.result()).isEqualTo(VerificationResult.REJECTED);
        assertThat(verification.reason()).isEqualTo(VerificationReason.ADDRESS_NOT_GEOCODABLE);
    }

    @Test
    void staysPendingWhenTheGeocoderDoesNotAnswer() {
        var verification = evaluate(Optional.of(ACTIVE_HABIDO), GeocodingResult::unavailable);

        assertThat(verification.result()).isEqualTo(VerificationResult.PENDING);
        assertThat(verification.reason()).isEqualTo(VerificationReason.GEOCODER_UNAVAILABLE);
    }

    @Test
    void readsStatusAndConditionWithoutCaringAboutCaseOrSpaces() {
        var lowerCase = new Taxpayer(RUC, "X", " activo ", "habido");

        assertThat(lowerCase.isActive()).isTrue();
        assertThat(lowerCase.isHabido()).isTrue();
    }

    @Test
    void anApprovedVerificationCannotCarryAReason() {
        assertThatThrownBy(() -> new RucVerification(null, PROFILE_ID, RUC, VerificationResult.APPROVED,
                VerificationReason.RUC_NOT_FOUND, null, null, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    private static RucVerification evaluate(Optional<Taxpayer> taxpayer, Supplier<GeocodingResult> geocoder) {
        return RucVerification.evaluate(PROFILE_ID, RUC, taxpayer, geocoder, NOW);
    }

    private static final class CountingGeocoder implements Supplier<GeocodingResult> {
        private final GeocodingResult result;
        private final AtomicInteger calls = new AtomicInteger();

        private CountingGeocoder(GeocodingResult result) {
            this.result = result;
        }

        @Override
        public GeocodingResult get() {
            calls.incrementAndGet();
            return result;
        }

        int calls() {
            return calls.get();
        }
    }
}

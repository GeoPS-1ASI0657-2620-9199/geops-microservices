package com.geopslabs.geops.identity.domain.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessProfileTest {
    private static final Long OWNER_ID = 42L;
    private static final Ruc RUC = new Ruc("10456789019");
    private static final GeoPoint LOCATION = new GeoPoint(-12.0681, -77.0350);
    private static final String ADDRESS = "Jr. Huánuco 1250, La Victoria";

    @Test
    void startsActiveAndUnverified() {
        var profile = register(RUC, ADDRESS);

        assertThat(profile.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
    }

    @Test
    void belongsToItsOwnerAfterOwnedBy() {
        var profile = register(RUC, ADDRESS).ownedBy(OWNER_ID);

        assertThat(profile.getUserId()).isEqualTo(OWNER_ID);
        assertThat(profile.getRuc()).isEqualTo(RUC);
        assertThat(profile.getLocation()).isEqualTo(LOCATION);
    }

    @Test
    void rejectsMalformedRuc() {
        assertThatThrownBy(() -> register(new Ruc("123"), ADDRESS)).isInstanceOf(InvalidRucException.class);
    }

    @Test
    void rejectsMissingAddress() {
        assertThatThrownBy(() -> register(RUC, null))
                .isInstanceOf(InvalidLocationException.class)
                .hasMessage("Falta la dirección. Corrige la ubicación del local.");
    }

    @Test
    void anApprovalGrantsTheSeal() {
        var profile = register(RUC, ADDRESS);

        assertThat(profile.applyVerification(verification(VerificationResult.APPROVED, null)))
                .isEqualTo(SealChange.GRANTED);
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(profile.isVerified()).isTrue();
    }

    @Test
    void anApprovalOfAVerifiedBusinessConfirmsTheSeal() {
        var profile = withStatus(VerificationStatus.VERIFIED);

        assertThat(profile.applyVerification(verification(VerificationResult.APPROVED, null)))
                .isEqualTo(SealChange.CONFIRMED);
        assertThat(SealChange.CONFIRMED.mustBePublished()).isTrue();
    }

    @Test
    void aRejectionOfAVerifiedBusinessRevokesTheSeal() {
        var profile = withStatus(VerificationStatus.VERIFIED);

        var change = profile.applyVerification(verification(VerificationResult.REJECTED,
                VerificationReason.TAXPAYER_NOT_ACTIVE));

        assertThat(change).isEqualTo(SealChange.REVOKED);
        assertThat(change.mustBePublished()).isTrue();
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    void aRejectionOfABusinessThatNeverHadTheSealChangesNothingToPublish() {
        var profile = register(RUC, ADDRESS);

        var change = profile.applyVerification(verification(VerificationResult.REJECTED,
                VerificationReason.RUC_NOT_FOUND));

        assertThat(change).isEqualTo(SealChange.UNCHANGED);
        assertThat(change.mustBePublished()).isFalse();
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    void aPendingVerificationNeverGrantsTheSeal() {
        var profile = register(RUC, ADDRESS);

        var change = profile.applyVerification(verification(VerificationResult.PENDING,
                VerificationReason.GEOCODER_UNAVAILABLE));

        assertThat(change).isEqualTo(SealChange.UNCHANGED);
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    void aVerifiedBusinessKeepsTheSealWhileTheGeocoderDoesNotAnswer() {
        var profile = withStatus(VerificationStatus.VERIFIED);

        var change = profile.applyVerification(verification(VerificationResult.PENDING,
                VerificationReason.GEOCODER_UNAVAILABLE));

        assertThat(change).isEqualTo(SealChange.UNCHANGED);
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    void correctsTheAddressAndKeepsTheMapPoint() {
        var profile = register(RUC, ADDRESS);

        profile.correctAddress("  Jr. Huánuco 1252, La Victoria ");

        assertThat(profile.getAddress()).isEqualTo("Jr. Huánuco 1252, La Victoria");
        assertThat(profile.getLocation()).isEqualTo(LOCATION);
    }

    @Test
    void aVerifiedBusinessIgnoresACorrectedAddressThatWasNotGeocoded() {
        var profile = withStatus(VerificationStatus.VERIFIED);

        var change = profile.applyVerification(verification(VerificationResult.REJECTED,
                VerificationReason.ADDRESS_NOT_GEOCODABLE), "Av. Larcco 999, Miraflores");

        assertThat(change).isEqualTo(SealChange.UNCHANGED);
        assertThat(profile.getAddress()).isEqualTo(ADDRESS);
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    void aBusinessWithoutSealKeepsTheCorrectedAddressEvenIfItIsRejected() {
        var profile = register(RUC, ADDRESS);

        profile.applyVerification(verification(VerificationResult.REJECTED,
                VerificationReason.ADDRESS_NOT_GEOCODABLE), "Av. Larcco 999, Miraflores");

        assertThat(profile.getAddress()).isEqualTo("Av. Larcco 999, Miraflores");
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    void aVerifiedBusinessTakesACorrectedAddressThatWasGeocoded() {
        var profile = withStatus(VerificationStatus.VERIFIED);

        var change = profile.applyVerification(verification(VerificationResult.APPROVED, null),
                "Av. José Larco 345, Miraflores");

        assertThat(change).isEqualTo(SealChange.CONFIRMED);
        assertThat(profile.getAddress()).isEqualTo("Av. José Larco 345, Miraflores");
    }

    @Test
    void rejectsABlankCorrectedAddress() {
        var profile = register(RUC, ADDRESS);

        assertThatThrownBy(() -> profile.correctAddress("  ")).isInstanceOf(InvalidLocationException.class);
        assertThat(profile.getAddress()).isEqualTo(ADDRESS);
    }

    private static RucVerification verification(VerificationResult result, VerificationReason reason) {
        return new RucVerification(1L, 84L, RUC, result, reason, "ACTIVO", "HABIDO",
                java.time.Instant.parse("2026-10-21T15:04:00Z"));
    }

    private static BusinessProfile withStatus(VerificationStatus status) {
        return new BusinessProfile(84L, OWNER_ID, register(RUC, ADDRESS), AccountStatus.ACTIVE, status);
    }

    private static BusinessProfile register(Ruc ruc, String address) {
        return BusinessProfile.register("Bodega Doña Rosa", "Bodega", ruc, address, LOCATION,
                "Lun-Sáb 07:00-22:00");
    }
}

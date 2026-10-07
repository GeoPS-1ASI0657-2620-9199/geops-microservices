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

    private static BusinessProfile register(Ruc ruc, String address) {
        return BusinessProfile.register("Bodega Doña Rosa", "Bodega", ruc, address, LOCATION,
                "Lun-Sáb 07:00-22:00");
    }
}

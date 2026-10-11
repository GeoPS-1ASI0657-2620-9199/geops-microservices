package com.geopslabs.geops.identity.domain.models;

import java.util.Locale;

/**
 * A taxpayer as the SUNAT reduced registry lists it. Status and condition keep the registry's own words (for example
 * "BAJA DE OFICIO" or "NO HABIDO") so a rejection can tell the owner what SUNAT says.
 */
public record Taxpayer(Ruc ruc, String businessName, String status, String condition) {
    private static final String ACTIVE = "ACTIVO";
    private static final String HABIDO = "HABIDO";

    public boolean isActive() {
        return ACTIVE.equals(normalized(status));
    }

    public boolean isHabido() {
        return HABIDO.equals(normalized(condition));
    }

    private static String normalized(String value) {
        return value == null ? null : value.strip().toUpperCase(Locale.ROOT);
    }
}

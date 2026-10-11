package com.geopslabs.geops.identity.infrastructure.sunat;

import java.util.List;

/**
 * One well-formed line of the registry, with the fields Identity keeps plus the ubigeo used to filter it.
 */
public record SunatTaxpayerLine(String ruc, String businessName, String status, String condition, String ubigeo) {

    public boolean isInAnyOf(List<String> ubigeoPrefixes) {
        return ubigeoPrefixes.stream().anyMatch(ubigeo::startsWith);
    }
}

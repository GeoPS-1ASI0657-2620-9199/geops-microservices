package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.UnavailableException;

/**
 * The SUNAT registry has never been loaded. Verifying now would reject every RUC as nonexistent.
 */
public class TaxpayerRegistryEmptyException extends UnavailableException {
    private static final String CODE = "TAXPAYER_REGISTRY_EMPTY";
    private static final String MESSAGE = "Todavía no podemos verificar RUC. Inténtalo de nuevo en unos minutos.";

    public TaxpayerRegistryEmptyException() {
        super(CODE, MESSAGE);
    }
}

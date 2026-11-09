package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.Taxpayer;

import java.util.Optional;

/**
 * Identity's copy of the SUNAT reduced registry. The verification never calls SUNAT online.
 */
public interface TaxpayerRegistryPort {
    Optional<Taxpayer> findByRuc(Ruc ruc);

    /** False until the first load finishes; with an empty registry every RUC would look nonexistent. */
    boolean isLoaded();
}

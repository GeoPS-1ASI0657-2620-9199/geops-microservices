package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.Taxpayer;
import com.geopslabs.geops.identity.domain.models.TaxpayerRegistryUnavailableException;

import java.util.Optional;

/**
 * Where Identity reads a taxpayer: its copy of the SUNAT reduced registry or the online RUC lookup.
 */
public interface TaxpayerRegistryPort {
    /**
     * @return the taxpayer, or empty when SUNAT does not know the RUC
     * @throws TaxpayerRegistryUnavailableException when the source did not give an answer
     */
    Optional<Taxpayer> findByRuc(Ruc ruc);

    /** False until the first load finishes; with an empty registry every RUC would look nonexistent. */
    boolean isLoaded();
}

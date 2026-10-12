package com.geopslabs.geops.catalog.application.usecases;

import java.util.List;

/**
 * US07 and E2-09: the administrator loads offers of public sources in a batch; they wait for coordinates.
 */
public interface LoadPublicOffersUseCase {
    List<Long> load(LoadPublicOffersCommand command);
}

package com.geopslabs.geops.catalog.infrastructure.cache;

import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;
import com.geopslabs.geops.catalog.domain.ports.CellCachePort;

import java.util.List;
import java.util.Set;

/**
 * geops.cache.enabled=false: the search runs the radius query of Sprint 1 and there is nothing to evict.
 */
public class DisabledCellCache implements CellCachePort {
    @Override
    public Lookup lookup(Set<String> geohashes) {
        return Lookup.empty();
    }

    @Override
    public void put(String geohash, String version, List<NearbyOfferCandidate> offers) {
        // Nothing is cached.
    }

    @Override
    public void evict(Set<String> geohashes) {
        // Nothing is cached.
    }
}

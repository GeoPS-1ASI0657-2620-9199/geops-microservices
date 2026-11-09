package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cache of the catalog by geohash cell (Módulo de Caché por Celda). It never throws: a cache that does not answer
 * behaves as a cache with no cells, so the search goes to the database.
 *
 * <p>Each lookup also returns the version of the cells it missed. {@link #put} stores a cell only if its version
 * did not change since the lookup, so a search that read the database before a publication cannot write back the
 * old cell after {@link #evict} removed it.</p>
 */
public interface CellCachePort {

    Lookup lookup(Set<String> geohashes);

    void put(String geohash, String version, List<NearbyOfferCandidate> offers);

    void evict(Set<String> geohashes);

    /** hits: the cells found; versions: the version of every cell asked for (null when it has none). */
    record Lookup(Map<String, List<NearbyOfferCandidate>> hits, Map<String, String> versions) {
        public static Lookup empty() {
            return new Lookup(Map.of(), Map.of());
        }
    }
}

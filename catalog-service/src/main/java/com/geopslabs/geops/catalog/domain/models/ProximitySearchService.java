package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.ports.CellCachePort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Búsqueda por proximidad (Informe 4.3.2.5). With the cell cache it asks the cache for the geohash cells that cover
 * the circle, reads from the database only the cells that are missing, stores them, and then applies in memory what
 * does not depend on the cell: validity in Lima today, exact distance, campaign zone, category and order. Without
 * the cache (geops.cache.enabled=false) it runs the radius query of Sprint 1.
 */
public class ProximitySearchService {
    public static final int DISTANCE_BAND_METERS = 100;

    private static final Comparator<RankedOffer> RANKING = Comparator
            .comparingInt((RankedOffer ranked) -> (int) (ranked.distanceMeters() / DISTANCE_BAND_METERS))
            .thenComparing(ranked -> ranked.offer().hasOpenReports())
            .thenComparing(ranked -> !ranked.offer().verifiedSeal())
            .thenComparingDouble(RankedOffer::distanceMeters);

    private final OfferRepositoryPort offers;
    private final CellCachePort cellCache;
    private final Clock clock;

    /** Search straight against the database, as in Sprint 1. */
    public ProximitySearchService(OfferRepositoryPort offers, Clock clock) {
        this(offers, null, clock);
    }

    /** Search through the cell cache. */
    public ProximitySearchService(OfferRepositoryPort offers, CellCachePort cellCache, Clock clock) {
        this.offers = offers;
        this.cellCache = cellCache;
        this.clock = clock;
    }

    public List<RankedOffer> search(GeoPoint origin, int radiusMeters, String category) {
        var today = LocalDate.now(clock);
        return candidates(origin, radiusMeters, today).stream()
                .filter(candidate -> !candidate.validTo().isBefore(today))
                .filter(candidate -> category == null || category.equals(candidate.category()))
                .map(candidate -> RankedOffer.of(candidate, origin.distanceTo(candidate.location())))
                .filter(ranked -> ranked.distanceMeters() <= radiusMeters)
                .filter(ranked -> ranked.offer().zone().covers(origin))
                .sorted(RANKING)
                .toList();
    }

    private List<NearbyOfferCandidate> candidates(GeoPoint origin, int radiusMeters, LocalDate today) {
        if (cellCache == null) {
            return offers.findPublishedWithin(origin, radiusMeters, today);
        }
        var cells = GeohashCover.cover(origin, radiusMeters);
        var geohashes = cells.stream().map(GeoCell::geohash).collect(Collectors.toCollection(TreeSet::new));
        var lookup = lookupQuietly(geohashes);
        var found = new ArrayList<NearbyOfferCandidate>();
        lookup.hits().values().forEach(found::addAll);

        var missing = cells.stream().filter(cell -> !lookup.hits().containsKey(cell.geohash()))
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            found.addAll(loadAndStore(missing, lookup.versions(), today));
        }
        return found;
    }

    private List<NearbyOfferCandidate> loadAndStore(Set<GeoCell> missing, Map<String, String> versions,
                                                    LocalDate today) {
        var loaded = offers.findPublishedInCells(missing, today);
        var found = new ArrayList<NearbyOfferCandidate>();
        for (var cell : missing) {
            var cellOffers = loaded.getOrDefault(cell.geohash(), List.of());
            putQuietly(cell.geohash(), versions.get(cell.geohash()), cellOffers);
            found.addAll(cellOffers);
        }
        return found;
    }

    private CellCachePort.Lookup lookupQuietly(Set<String> geohashes) {
        try {
            return cellCache.lookup(geohashes);
        } catch (RuntimeException exception) {
            return CellCachePort.Lookup.empty();
        }
    }

    private void putQuietly(String geohash, String version, List<NearbyOfferCandidate> cellOffers) {
        try {
            cellCache.put(geohash, version, cellOffers);
        } catch (RuntimeException exception) {
            // The port never throws; if an adapter does, the search still answers from the database.
        }
    }
}

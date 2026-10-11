package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.ports.CellCachePort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachedProximitySearchTest {
    private static final GeoPoint ORIGIN = new GeoPoint(-12.1211, -77.0297);
    private static final int RADIUS = 800;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final double METERS_PER_DEGREE = Math.toRadians(1) * GeoPoint.EARTH_MEAN_RADIUS_METERS;

    private OfferRepositoryPort offers;
    private CellCachePort cache;
    private ProximitySearchService search;
    private Set<String> cells;

    @BeforeEach
    void setUp() {
        offers = mock(OfferRepositoryPort.class);
        cache = mock(CellCachePort.class);
        search = new ProximitySearchService(offers, cache, CLOCK);
        cells = GeohashCover.cover(ORIGIN, RADIUS).stream().map(GeoCell::geohash).collect(Collectors.toSet());
    }

    @Test
    void withEveryCellCachedTheDatabaseIsNotRead() {
        var nearby = candidate(1L, 200, TODAY.plusDays(3), "Gastronomía");
        when(cache.lookup(anySet())).thenReturn(cachedEverywhere(nearby));

        var result = search.search(ORIGIN, RADIUS, null);

        assertThat(result).extracting(ranked -> ranked.offer().offerId()).containsExactly(1L);
        verify(offers, never()).findPublishedInCells(any(), any());
        verify(offers, never()).findPublishedWithin(any(), any(Integer.class), any());
    }

    @Test
    void aMissingCellIsReadOnceAndStoredWithTheVersionOfTheLookup() {
        var nearby = candidate(1L, 200, TODAY.plusDays(3), "Gastronomía");
        var missingCell = GeoCell.containing(nearby.location()).geohash();
        var hits = new HashMap<String, List<NearbyOfferCandidate>>();
        cells.stream().filter(cell -> !cell.equals(missingCell)).forEach(cell -> hits.put(cell, List.of()));
        when(cache.lookup(anySet())).thenReturn(new CellCachePort.Lookup(hits, Map.of(missingCell, "7")));
        when(offers.findPublishedInCells(Set.of(new GeoCell(missingCell)), TODAY))
                .thenReturn(Map.of(missingCell, List.of(nearby)));

        var result = search.search(ORIGIN, RADIUS, null);

        assertThat(result).hasSize(1);
        verify(offers, times(1)).findPublishedInCells(Set.of(new GeoCell(missingCell)), TODAY);
        verify(cache).put(missingCell, "7", List.of(nearby));
    }

    @Test
    void anEmptyCellIsCachedSoItIsNotReadAgain() {
        when(cache.lookup(anySet())).thenReturn(CellCachePort.Lookup.empty());
        when(offers.findPublishedInCells(any(), eq(TODAY))).thenReturn(Map.of());

        assertThat(search.search(ORIGIN, RADIUS, null)).isEmpty();

        verify(cache, times(cells.size())).put(any(), eq(null), eq(List.of()));
    }

    @Test
    void withTheCacheDownTheSearchGoesToTheDatabase() {
        var nearby = candidate(1L, 200, TODAY.plusDays(3), "Gastronomía");
        when(cache.lookup(anySet())).thenThrow(new IllegalStateException("Redis is down"));
        when(offers.findPublishedInCells(any(), eq(TODAY)))
                .thenReturn(Map.of(GeoCell.containing(nearby.location()).geohash(), List.of(nearby)));

        assertThat(search.search(ORIGIN, RADIUS, null)).hasSize(1);
    }

    @Test
    void aCachedOfferThatExpiredSinceIsDropped() {
        var expired = candidate(1L, 200, TODAY.minusDays(1), "Gastronomía");
        var valid = candidate(2L, 300, TODAY, "Gastronomía");
        when(cache.lookup(anySet())).thenReturn(cachedEverywhere(expired, valid));

        assertThat(search.search(ORIGIN, RADIUS, null)).extracting(ranked -> ranked.offer().offerId())
                .containsExactly(2L);
    }

    @Test
    void theCategoryAndTheExactRadiusAreAppliedAfterJoiningTheCells() {
        var food = candidate(1L, 200, TODAY, "Gastronomía");
        var pets = candidate(2L, 250, TODAY, "Mascotas");
        var tooFar = candidate(3L, 850, TODAY, "Gastronomía");
        when(cache.lookup(anySet())).thenReturn(cachedEverywhere(food, pets, tooFar));

        assertThat(search.search(ORIGIN, RADIUS, "Gastronomía")).extracting(ranked -> ranked.offer().offerId())
                .containsExactly(1L);
    }

    private CellCachePort.Lookup cachedEverywhere(NearbyOfferCandidate... candidates) {
        var hits = new HashMap<String, List<NearbyOfferCandidate>>();
        cells.forEach(cell -> hits.put(cell, List.of()));
        for (var candidate : candidates) {
            var cell = GeoCell.containing(candidate.location()).geohash();
            var inCell = new java.util.ArrayList<>(hits.getOrDefault(cell, List.of()));
            inCell.add(candidate);
            hits.put(cell, inCell);
        }
        return new CellCachePort.Lookup(hits, Map.of());
    }

    private static NearbyOfferCandidate candidate(Long id, int metersNorth, LocalDate validTo, String category) {
        var location = new GeoPoint(ORIGIN.latitude() + metersNorth / METERS_PER_DEGREE, ORIGIN.longitude());
        return new NearbyOfferCandidate(id, "Oferta " + id, BigDecimal.TEN, validTo, category, "Av. Larco 345",
                null, location, 84L, "Comercio", false, 0, new CampaignZone(ZoneType.RADIUS, null, null, null));
    }
}

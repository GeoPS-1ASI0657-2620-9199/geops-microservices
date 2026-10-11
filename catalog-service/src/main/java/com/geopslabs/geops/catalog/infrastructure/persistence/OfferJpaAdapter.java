package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.GeoCell;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.PublicSourceOffer;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OfferJpaAdapter implements OfferRepositoryPort {
    private final OfferJpaRepository repository;

    public OfferJpaAdapter(OfferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Offer save(Offer offer) {
        return OfferPersistenceMapper.toDomain(repository.saveAndFlush(OfferPersistenceMapper.toEntity(offer)));
    }

    @Override
    public Optional<Offer> findById(Long id) {
        return repository.findById(id).map(OfferPersistenceMapper::toDomain);
    }

    @Override
    public List<Offer> findByCampaignId(Long campaignId) {
        return repository.findByCampaignIdOrderByIdAsc(campaignId).stream()
                .map(OfferPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<NearbyOfferCandidate> findPublishedWithin(GeoPoint origin, int radiusMeters, LocalDate today) {
        return repository.findPublishedWithin(origin.latitude(), origin.longitude(), radiusMeters, today,
                        OfferStatus.PUBLISHED.name(), CampaignStatus.ACTIVE.name()).stream()
                .map(OfferJpaAdapter::toCandidate)
                .toList();
    }

    @Override
    public Map<String, List<NearbyOfferCandidate>> findPublishedInCells(Set<GeoCell> cells, LocalDate today) {
        if (cells.isEmpty()) {
            return Map.of();
        }
        var envelope = GeoCell.envelopeOf(cells);
        var geohashes = cells.stream().map(GeoCell::geohash).toList();
        return repository.findPublishedInCells(envelope.center().latitude(), envelope.center().longitude(),
                        envelope.radiusMeters(), geohashes, today, OfferStatus.PUBLISHED.name(),
                        CampaignStatus.ACTIVE.name()).stream()
                .collect(Collectors.groupingBy(OfferJpaRepository.CellOfferRow::getCell,
                        Collectors.mapping(OfferJpaAdapter::toCandidate, Collectors.toList())));
    }

    @Override
    public List<PublicSourceOffer> findPublicSourceWithin(GeoPoint origin, int radiusMeters, LocalDate today) {
        return repository.findPublicSourceWithin(origin.latitude(), origin.longitude(), radiusMeters, today,
                        OfferSource.PUBLIC_SOURCE.name(), GeocodingStatus.GEOCODED.name(), OfferStatus.PUBLISHED.name())
                .stream()
                .map(row -> new PublicSourceOffer(row.getOfferId(), row.getTitle(), row.getSourceName(),
                        row.getPrice(), row.getValidTo(), row.getCategory(), row.getAddress(), row.getImageUrl(),
                        toPoint(row)))
                .toList();
    }

    @Override
    public List<GeoPoint> findPublishedLocationsOfBusiness(Long businessId, LocalDate today) {
        return repository.findLocationsOfBusiness(businessId, today, OfferStatus.PUBLISHED.name()).stream()
                .map(OfferJpaAdapter::toPoint)
                .toList();
    }

    @Override
    public List<GeoPoint> expirePublishedBefore(LocalDate today) {
        return repository.expirePublishedBefore(today, OfferStatus.PUBLISHED.name(), OfferStatus.EXPIRED.name())
                .stream()
                .map(OfferJpaAdapter::toPoint)
                .toList();
    }

    @Override
    public List<Offer> findPendingGeocoding() {
        return repository.findByGeocodingStatusOrderByIdAsc(GeocodingStatus.PENDING).stream()
                .map(OfferPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean markGeocoded(Long offerId, GeoPoint location) {
        return repository.markGeocoded(offerId, location.latitude(), location.longitude(),
                GeocodingStatus.GEOCODED.name(), GeocodingStatus.PENDING.name()) == 1;
    }

    @Override
    public boolean markForManualReview(Long offerId) {
        return repository.markForManualReview(offerId, GeocodingStatus.MANUAL_REVIEW.name(),
                GeocodingStatus.PENDING.name()) == 1;
    }

    @Override
    public Map<GeocodingStatus, Long> countPublicSourceByGeocodingStatus() {
        return repository.countByGeocodingStatus(OfferSource.PUBLIC_SOURCE.name()).stream()
                .collect(Collectors.toMap(row -> GeocodingStatus.valueOf(row.getStatus()),
                        OfferJpaRepository.StatusCountRow::getTotal));
    }

    private static GeoPoint toPoint(OfferJpaRepository.LocationRow row) {
        return new GeoPoint(row.getLatitude(), row.getLongitude());
    }

    private static NearbyOfferCandidate toCandidate(NearbyOfferRow row) {
        return new NearbyOfferCandidate(row.getOfferId(), row.getTitle(), row.getPrice(), row.getValidTo(),
                row.getCategory(), row.getAddress(), row.getImageUrl(), new GeoPoint(row.getLatitude(), row.getLongitude()), row.getBusinessId(),
                row.getBusinessName(), row.getVerifiedSeal(), row.getOpenReports(), zoneOf(row));
    }

    private static CampaignZone zoneOf(NearbyOfferRow row) {
        var center = row.getZoneLatitude() == null ? null
                : new GeoPoint(row.getZoneLatitude(), row.getZoneLongitude());
        return new CampaignZone(ZoneType.valueOf(row.getZoneType()), center, row.getZoneRadiusMeters(),
                row.getZoneDistrict());
    }
}

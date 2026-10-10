package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

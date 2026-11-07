package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.*;
import com.geopslabs.geops.engagement.domain.models.*;
import com.geopslabs.geops.engagement.domain.models.queries.GetFollowedBusinessesQuery;
import com.geopslabs.geops.engagement.domain.ports.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class FollowQueryService implements ListFollowedBusinessesUseCase {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final FollowRepositoryPort follows;
    private final BusinessSnapshotRepositoryPort businesses;
    private final OfferSnapshotRepositoryPort offers;
    private final Clock clock;

    public FollowQueryService(FollowRepositoryPort follows, BusinessSnapshotRepositoryPort businesses,
                              OfferSnapshotRepositoryPort offers, Clock clock) {
        this.follows = follows;
        this.businesses = businesses;
        this.offers = offers;
        this.clock = clock;
    }

    @Override
    public List<FollowedBusinessView> list(GetFollowedBusinessesQuery query) {
        return follows.findByConsumer(query.consumerId()).stream().map(this::view).toList();
    }

    public FollowedBusinessView view(Follow follow) {
        var active = offers.findAvailableByBusinessIds(List.of(follow.businessId()), LocalDate.now(clock.withZone(LIMA)));
        var name = businesses.findById(follow.businessId()).map(BusinessSnapshot::businessName).orElse(null);
        return new FollowedBusinessView(follow.businessId(), name, follow.followedAt(), active.size(), active);
    }
}

package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.*;
import com.geopslabs.geops.engagement.domain.models.queries.GetFollowedBusinessesQuery;
import com.geopslabs.geops.engagement.domain.ports.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FollowQueryServiceTest {
    @Test
    void readsActiveOffersForTheCurrentDayInLima() {
        var follows = mock(FollowRepositoryPort.class);
        var businesses = mock(BusinessSnapshotRepositoryPort.class);
        var offers = mock(OfferSnapshotRepositoryPort.class);
        var clock = Clock.fixed(Instant.parse("2026-10-28T02:00:00Z"), ZoneOffset.UTC);
        when(follows.findByConsumer(2001L)).thenReturn(List.of(new Follow(1L, 2001L, 84L, clock.instant())));
        when(businesses.findById(84L)).thenReturn(Optional.of(new BusinessSnapshot(84L, "Don Pepe")));
        when(offers.findAvailableByBusinessIds(List.of(84L), LocalDate.parse("2026-10-27")))
                .thenReturn(List.of(new OfferSnapshot(1052L, 84L, "2x1", LocalDate.parse("2026-11-15"), "PUBLISHED")));
        var result = new FollowQueryService(follows, businesses, offers, clock).list(new GetFollowedBusinessesQuery(2001L));
        assertThat(result).singleElement().satisfies(b -> {
            assertThat(b.businessName()).isEqualTo("Don Pepe");
            assertThat(b.activeOffersCount()).isEqualTo(1);
            assertThat(b.activeOffers()).extracting(OfferSnapshot::offerId).containsExactly(1052L);
        });
    }
}

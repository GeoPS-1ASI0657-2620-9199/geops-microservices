package com.geopslabs.geops.engagement.configuration;

import com.geopslabs.geops.engagement.application.services.*;
import com.geopslabs.geops.engagement.domain.ports.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class FollowConfiguration {
    @Bean
    FollowQueryService followQueryService(FollowRepositoryPort follows, BusinessSnapshotRepositoryPort businesses,
                                         OfferSnapshotRepositoryPort offers, Clock clock) {
        return new FollowQueryService(follows, businesses, offers, clock);
    }

    @Bean
    FollowCommandService followCommandService(FollowRepositoryPort follows, BusinessSnapshotRepositoryPort businesses,
                                             OutboxPort outbox, FollowQueryService queries, Clock clock) {
        return new FollowCommandService(follows, businesses, outbox, queries, clock);
    }
}

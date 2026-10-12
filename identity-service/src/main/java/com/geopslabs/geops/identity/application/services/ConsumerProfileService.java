package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.ChangeSearchRadiusUseCase;
import com.geopslabs.geops.identity.application.usecases.GetConsumerProfileUseCase;
import com.geopslabs.geops.identity.domain.events.ConsumerPreferencesChanged;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.ConsumerProfileNotFoundException;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The radius a consumer with a session chose (US46, E2-07). The profile and ConsumerPreferencesChanged are written
 * in the same transaction; saving the same radius again publishes nothing.
 */
@Transactional
public class ConsumerProfileService implements ChangeSearchRadiusUseCase, GetConsumerProfileUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerProfileService.class);

    private final ConsumerProfileRepositoryPort consumerProfiles;
    private final OutboxPort outbox;
    private final Clock clock;
    private final Supplier<UUID> eventIds;

    public ConsumerProfileService(ConsumerProfileRepositoryPort consumerProfiles, OutboxPort outbox, Clock clock,
                                  Supplier<UUID> eventIds) {
        this.consumerProfiles = consumerProfiles;
        this.outbox = outbox;
        this.clock = clock;
        this.eventIds = eventIds;
    }

    @Override
    public ConsumerProfile changeSearchRadius(Long consumerId, int radiusMinutes) {
        var profile = get(consumerId);
        if (!profile.changeSearchRadius(radiusMinutes)) {
            return profile;
        }
        var saved = consumerProfiles.save(profile);
        outbox.append(ConsumerPreferencesChanged.of(saved, eventIds.get(), clock.instant()));
        LOGGER.info("consumer.search-radius.changed consumerId={} radiusMinutes={}", consumerId, radiusMinutes);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsumerProfile get(Long consumerId) {
        return consumerProfiles.findByUserId(consumerId).orElseThrow(ConsumerProfileNotFoundException::new);
    }
}

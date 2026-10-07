package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.usecases.RecordLastKnownLocationUseCase;
import com.geopslabs.geops.notification.application.usecases.RecordedLocation;
import com.geopslabs.geops.notification.domain.models.GeoPoint;
import com.geopslabs.geops.notification.domain.models.LastKnownLocation;
import com.geopslabs.geops.notification.domain.models.NotificationPreference;
import com.geopslabs.geops.notification.domain.models.commands.RecordLocationCommand;
import com.geopslabs.geops.notification.domain.models.exceptions.InvalidCaptureTimeException;
import com.geopslabs.geops.notification.domain.models.exceptions.RecipientNotFoundException;
import com.geopslabs.geops.notification.domain.ports.LastKnownLocationRepositoryPort;
import com.geopslabs.geops.notification.domain.ports.NotificationPreferenceRepositoryPort;
import com.geopslabs.geops.notification.domain.ports.RecipientRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class LastKnownLocationCommandService implements RecordLastKnownLocationUseCase {
    public static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(2);
    public static final int DEFAULT_RADIUS_METERS = 800;
    private static final Logger LOGGER = LoggerFactory.getLogger(LastKnownLocationCommandService.class);

    private final RecipientRepositoryPort recipientRepository;
    private final NotificationPreferenceRepositoryPort preferenceRepository;
    private final LastKnownLocationRepositoryPort locationRepository;
    private final Clock clock;

    public LastKnownLocationCommandService(RecipientRepositoryPort recipientRepository,
                                           NotificationPreferenceRepositoryPort preferenceRepository,
                                           LastKnownLocationRepositoryPort locationRepository, Clock clock) {
        this.recipientRepository = recipientRepository;
        this.preferenceRepository = preferenceRepository;
        this.locationRepository = locationRepository;
        this.clock = clock;
    }

    @Override
    public RecordedLocation record(RecordLocationCommand command) {
        if (!recipientRepository.existsConsumer(command.consumerId())) {
            LOGGER.info("location.rejected reason=no-recipient");
            throw new RecipientNotFoundException();
        }
        var now = now();
        var capturedAt = LocalDateTime.ofInstant(command.capturedAt(), ZoneOffset.UTC);
        if (capturedAt.isAfter(now.plus(MAX_CLOCK_SKEW))) {
            LOGGER.info("location.rejected reason=future-capture-time");
            throw new InvalidCaptureTimeException();
        }
        preferenceRepository.createIfAbsent(NotificationPreference.initialFor(command.consumerId(), now));
        var reading = new LastKnownLocation(new GeoPoint(command.latitude(), command.longitude()),
                command.accuracyMeters(), DEFAULT_RADIUS_METERS, capturedAt);
        var current = locationRepository.saveIfNewer(command.consumerId(), reading);
        var fresh = current.isFresh(now);
        LOGGER.info("location.recorded fresh={}", fresh);
        return new RecordedLocation(command.consumerId(), current, fresh);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}

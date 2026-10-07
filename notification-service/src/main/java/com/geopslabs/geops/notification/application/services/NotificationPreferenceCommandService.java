package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.usecases.UpdateNotificationPreferencesUseCase;
import com.geopslabs.geops.notification.domain.models.NotificationPreference;
import com.geopslabs.geops.notification.domain.models.commands.UpdatePreferencesCommand;
import com.geopslabs.geops.notification.domain.models.exceptions.RecipientNotFoundException;
import com.geopslabs.geops.notification.domain.ports.NotificationPreferenceRepositoryPort;
import com.geopslabs.geops.notification.domain.ports.RecipientRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class NotificationPreferenceCommandService implements UpdateNotificationPreferencesUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationPreferenceCommandService.class);

    private final RecipientRepositoryPort recipientRepository;
    private final NotificationPreferenceRepositoryPort preferenceRepository;
    private final Clock clock;

    public NotificationPreferenceCommandService(RecipientRepositoryPort recipientRepository,
                                                NotificationPreferenceRepositoryPort preferenceRepository,
                                                Clock clock) {
        this.recipientRepository = recipientRepository;
        this.preferenceRepository = preferenceRepository;
        this.clock = clock;
    }

    @Override
    public NotificationPreference update(UpdatePreferencesCommand command) {
        if (!recipientRepository.existsConsumer(command.consumerId())) {
            LOGGER.info("preferences.rejected reason=no-recipient");
            throw new RecipientNotFoundException();
        }
        var preference = new NotificationPreference(command.consumerId(), command.pushEnabled(),
                command.emailEnabled(), command.dailyLimit(), now());
        var saved = preferenceRepository.upsert(preference);
        LOGGER.info("preferences.updated push={} email={} dailyLimit={}", saved.getPushEnabled(),
                saved.getEmailEnabled(), saved.getDailyLimit());
        return saved;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}

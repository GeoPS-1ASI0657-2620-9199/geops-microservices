package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.Recipient;
import java.time.Instant;
public interface ApplyIdentityEventsUseCase {
    void userRegistered(Recipient recipient, Instant occurredAt);
    void consumerPreferencesChanged(Long consumerId, int searchRadiusMinutes);
}

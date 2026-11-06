package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.*;
import com.geopslabs.geops.notification.application.exceptions.MissingLocalCopyException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipientProjectorServiceTest {
    private final RecipientRepositoryPort recipients = mock(RecipientRepositoryPort.class);
    private final NotificationPreferenceRepositoryPort preferences = mock(NotificationPreferenceRepositoryPort.class);
    private final RecipientProjectionPort projections = mock(RecipientProjectionPort.class);
    private final RecipientProjectorService service = new RecipientProjectorService(recipients, preferences, projections);
    @Test
    void aNewConsumerHasBothChannelsOffWithoutOverwritingExistingChoices() {
        var recipient = new Recipient(2001L, "local@example.test", false, "CONSUMER");
        service.userRegistered(recipient, Instant.parse("2026-10-27T14:00:00Z"));
        verify(recipients).upsert(recipient);
        verify(preferences).createIfAbsent(argThat(p -> !p.getPushEnabled() && !p.getEmailEnabled() && p.getDailyLimit() == 3));
        verify(preferences, never()).upsert(any());
    }
    @Test
    void ownerDoesNotReceiveConsumerPreferences() {
        service.userRegistered(new Recipient(9L, "owner@example.test", false, "BUSINESS_OWNER", 84L), Instant.now());
        verifyNoInteractions(preferences);
    }
    @Test
    void followWithoutRecipientIsRetriedAndRadiusUsesWalkingSpeed() {
        assertThatThrownBy(() -> service.businessFollowed(2001L, 84L)).isInstanceOf(MissingLocalCopyException.class);
        verifyNoInteractions(projections);
        service.consumerPreferencesChanged(2001L, 10);
        verify(projections).updateRadius(2001L, 800);
    }
}

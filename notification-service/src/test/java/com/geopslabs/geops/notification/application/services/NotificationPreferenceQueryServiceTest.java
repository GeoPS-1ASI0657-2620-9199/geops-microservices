package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.models.exceptions.RecipientNotFoundException;
import com.geopslabs.geops.notification.domain.ports.NotificationPreferenceRepositoryPort;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class NotificationPreferenceQueryServiceTest {
    @Test
    void returnsTheExistingChoiceAndMissingRecipientsAreNotInitializedByReading() {
        var repository = mock(NotificationPreferenceRepositoryPort.class);
        var preferences = new NotificationPreference(2001L, false, true, 2, LocalDateTime.now());
        when(repository.findByConsumerId(2001L)).thenReturn(Optional.of(preferences));
        var service = new NotificationPreferenceQueryService(repository);
        assertThat(service.get(2001L)).isSameAs(preferences);
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(RecipientNotFoundException.class);
        verify(repository, never()).upsert(any());
    }
}

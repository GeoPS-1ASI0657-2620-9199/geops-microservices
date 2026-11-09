package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.EmailUnsubscriptionRepositoryPort;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailUnsubscriptionServiceTest {
    @Test
    void recordsOnlyTheSignedEmailType() {
        var repository = mock(EmailUnsubscriptionRepositoryPort.class);
        when(repository.saveIfAbsent(any())).thenAnswer(i -> i.getArgument(0));
        var service = new EmailUnsubscriptionService(repository, "test-key", Clock.systemUTC());
        var result = service.unsubscribe(UnsubscribeToken.issue(2001L, NotificationType.NEARBY_OFFER, "test-key"));
        assertThat(result.consumerId()).isEqualTo(2001L);
        assertThat(result.notificationType()).isEqualTo(NotificationType.NEARBY_OFFER);
        verify(repository).saveIfAbsent(any());
    }
}

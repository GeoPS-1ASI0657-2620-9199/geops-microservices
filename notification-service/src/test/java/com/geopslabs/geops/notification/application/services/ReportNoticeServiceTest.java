package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.ports.NotificationChannelStrategy;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.NoticePlanningRepositoryPort;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;

class ReportNoticeServiceTest {
    @Test
    void reportsGoToThePartiesWithoutCheckingOfferConsentOrQuota() {
        var notices = mock(NoticePlanningRepositoryPort.class); var email = mock(NotificationChannelStrategy.class);
        var owner = new Recipient(9L, "owner@example.test", false, "BUSINESS_OWNER", 84L);
        when(notices.businessOwner(84L)).thenReturn(Optional.of(owner)); when(notices.recipient(9L)).thenReturn(Optional.of(owner));
        when(notices.save(any())).thenAnswer(i -> i.getArgument(0)); when(email.channel()).thenReturn(Channel.EMAIL); when(email.send(any(), any())).thenReturn(DeliveryStatus.SENT);
        new ReportNoticeService(notices, List.of(email), new TestTransactions(), Clock.systemUTC()).apply(new ReportNoticeEvent("ReportOpened", 31L, 2001L, 84L, "Q4ZT8K2M", "2x1", null));
        verify(email).send(argThat(n -> n.getType() == NotificationType.REPORT_RECEIVED && n.getRelatedEntityId().equals("report-31")), eq(owner));
        verify(notices, never()).lockPreferences(any()); verify(notices, never()).countOfferNoticesSince(any(), any(), any());
    }
}

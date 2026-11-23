package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.domain.models.Channel;
import com.geopslabs.geops.notification.domain.models.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationFactoryServiceTest {
    private final NotificationFactoryService factory = new NotificationFactoryService();

    @Test
    void createsANearbyOfferNoticeWithTitleMessageAndRelatedEntity() {
        var notification = factory.create(NotificationType.NEARBY_OFFER, Map.of(
                NotificationFactoryService.RECIPIENT_ID, "1",
                NotificationFactoryService.CHANNEL, Channel.EMAIL.name(),
                NotificationFactoryService.RELATED_ENTITY_ID, "1",
                NotificationFactoryService.BUSINESS_NAME, "Bodega Doña Rosa",
                NotificationFactoryService.OFFER_TITLE, "Menú ejecutivo a mitad de precio"));

        assertThat(notification.getRecipientId()).isEqualTo(1L);
        assertThat(notification.getType()).isEqualTo(NotificationType.NEARBY_OFFER);
        assertThat(notification.getChannel()).isEqualTo(Channel.EMAIL);
        assertThat(notification.getTitle()).isEqualTo("Oferta cerca de ti");
        assertThat(notification.getMessage()).contains("Bodega Doña Rosa", "Menú ejecutivo a mitad de precio");
        assertThat(notification.getRelatedEntityId()).isEqualTo("1");
        assertThat(notification.getStatus()).isNull();
    }

    @Test
    void reportNoticesKeepTheirAccents() {
        var event = new com.geopslabs.geops.notification.domain.models.ReportNoticeEvent("report.received", 7L, 1L, 2L, "RES-1", "2x1", "CONSUMER_FAVOR");
        var now = java.time.Instant.parse("2026-11-23T15:00:00Z");
        assertThat(NotificationFactoryService.report(9L, event, NotificationType.REPORT_RECEIVED, now).getMessage()).contains("reportó");
        assertThat(NotificationFactoryService.report(9L, event, NotificationType.REPORT_ANSWERED, now).getMessage()).contains("respondió");
        assertThat(NotificationFactoryService.report(9L, event, NotificationType.REPORT_RESOLVED, now).getMessage()).contains("cerró").doesNotContain("?");
    }

    @Test
    void rejectsANoticeWithoutTheDataItsMessageNeeds() {
        var data = Map.of(NotificationFactoryService.RECIPIENT_ID, "1",
                NotificationFactoryService.CHANNEL, Channel.WEB_PUSH.name());

        assertThatThrownBy(() -> factory.create(NotificationType.NEARBY_OFFER, data))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

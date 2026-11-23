package com.geopslabs.geops.identity.infrastructure.messaging;

import com.geopslabs.geops.identity.domain.events.BusinessVerified;
import com.geopslabs.geops.identity.domain.events.ConsumerPreferencesChanged;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.VerificationReason;
import com.geopslabs.geops.identity.domain.models.VerificationResult;
import com.geopslabs.geops.identity.infrastructure.persistence.OutboxEventJpaEntity;
import com.geopslabs.geops.identity.infrastructure.persistence.OutboxEventJpaRepository;
import com.geopslabs.geops.identity.infrastructure.persistence.OutboxJpaAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * BusinessVerified (read by Catalog) and ConsumerPreferencesChanged (read by Notification) have the shape of their
 * examples in platform/messaging/events. Notification reads searchRadiusMinutes, so a rename on this side fails here
 * before it sends every radius change to the dead letter topic.
 */
class IdentityEventsContractTest {
    private static final Instant NOW = Instant.parse("2026-10-21T15:04:00Z");

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxJpaAdapter adapter = new OutboxJpaAdapter(repository, Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void anApprovedBusinessVerifiedHasTheShapeOfTheExample() {
        adapter.append(new BusinessVerified(UUID.randomUUID(), NOW, new BusinessVerified.Payload(84L,
                "Restaurante Don Pepe", true, VerificationResult.APPROVED, null, NOW)));

        EventContract.assertSameShape(written().getPayload(), "BusinessVerified");
        assertThat(written().getAggregateId()).isEqualTo(84L);
        assertThat(written().getPayload()).contains("\"verified\":true", "\"result\":\"APPROVED\"",
                "\"verifiedAt\":\"2026-10-21T15:04:00Z\"");
    }

    @Test
    void aRejectedBusinessVerifiedCarriesTheReasonAsText() {
        adapter.append(new BusinessVerified(UUID.randomUUID(), NOW, new BusinessVerified.Payload(84L,
                "Restaurante Don Pepe", false, VerificationResult.REJECTED, VerificationReason.TAXPAYER_NOT_HABIDO,
                NOW)));

        EventContract.assertSameShape(written().getPayload(), "BusinessVerified");
        assertThat(written().getPayload()).contains("\"verified\":false", "\"reason\":\"TAXPAYER_NOT_HABIDO\"");
    }

    @Test
    void consumerPreferencesChangedHasTheShapeOfTheExample() {
        var profile = new ConsumerProfile(2001L, false, 10, null);

        adapter.append(ConsumerPreferencesChanged.of(profile, UUID.randomUUID(), NOW));

        EventContract.assertSameShape(written().getPayload(), "ConsumerPreferencesChanged");
        assertThat(written().getAggregateId()).isEqualTo(2001L);
        assertThat(written().getPayload()).contains("\"consumerId\":2001", "\"searchRadiusMinutes\":10");
    }

    private OutboxEventJpaEntity written() {
        var saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }
}

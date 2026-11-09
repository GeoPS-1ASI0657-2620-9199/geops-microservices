package com.geopslabs.geops.identity.infrastructure.messaging;

import com.geopslabs.geops.identity.domain.events.UserRegistered;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
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
import static org.mockito.Mockito.when;

class UserRegisteredContractTest {
    private static final Instant NOW = Instant.parse("2026-10-27T14:00:00Z");

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxJpaAdapter adapter = new OutboxJpaAdapter(repository, Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void aConsumerRegistrationHasTheShapeOfTheExample() {
        adapter.append(UserRegistered.of(user(2001L, Role.CONSUMER, "lucia.fernandez@ejemplo.pe"), null,
                UUID.randomUUID(), NOW));

        EventContract.assertSameShape(written().getPayload(), "UserRegistered");
        assertThat(written().getAggregateId()).isEqualTo(2001L);
        assertThat(written().getPayload()).contains("\"businessId\":null", "\"emailConfirmed\":false",
                "\"registeredAt\":\"2026-10-27T14:00:00Z\"");
    }

    @Test
    void aBusinessOwnerCarriesTheIdOfTheBusinessProfile() {
        var profile = mock(BusinessProfile.class);
        when(profile.getId()).thenReturn(84L);

        adapter.append(UserRegistered.of(user(2002L, Role.BUSINESS_OWNER, "rosa.quispe@ejemplo.pe"), profile,
                UUID.randomUUID(), NOW));

        EventContract.assertSameShape(written().getPayload(), "UserRegistered");
        assertThat(written().getPayload()).contains("\"businessId\":84", "\"role\":\"BUSINESS_OWNER\"");
    }

    @Test
    void theCorrelationIdOfTheRequestTravelsInTheEnvelopeWithoutChangingItsShape() {
        org.slf4j.MDC.put("correlationId", "req-42");
        try {
            adapter.append(UserRegistered.of(user(2001L, Role.CONSUMER, "lucia.fernandez@ejemplo.pe"), null,
                    UUID.randomUUID(), NOW));
        } finally {
            org.slf4j.MDC.remove("correlationId");
        }

        assertThat(written().getPayload()).contains("\"correlationId\":\"req-42\"");
    }

    private static User user(Long id, Role role, String email) {
        return new User(id, User.register("Nombre Apellido", new Email(email), "987123456", "hash", role), null, 0,
                null, NOW);
    }

    private OutboxEventJpaEntity written() {
        var saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }
}

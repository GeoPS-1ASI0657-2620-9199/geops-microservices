package com.geopslabs.geops.identity.infrastructure.messaging;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.geopslabs.geops.identity.domain.events.EmailConfirmed;
import com.geopslabs.geops.identity.domain.events.UserRegistered;
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

/** US28: the two events that Notification reads from Identity keep the shape of their examples. */
class EmailConfirmedContractTest {
    private static final Instant REGISTERED_AT = Instant.parse("2026-10-27T14:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-10-27T14:20:00Z");
    private static final String TOKEN = "q3Zt8b1xV0mWc9yRkP2sLhN4dF6gJ7aE5uT0iO3pQ1w";

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxJpaAdapter adapter = new OutboxJpaAdapter(repository, Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(),
            Clock.fixed(CONFIRMED_AT, ZoneOffset.UTC));

    @Test
    void emailConfirmedHasTheShapeOfTheExample() {
        adapter.append(EmailConfirmed.of(user(CONFIRMED_AT), UUID.randomUUID()));

        EventContract.assertSameShape(written().getPayload(), "EmailConfirmed");
        assertThat(written().getAggregateId()).isEqualTo(2001L);
        assertThat(written().getEventType()).isEqualTo("EmailConfirmed");
        assertThat(written().getPayload()).contains("\"confirmedAt\":\"2026-10-27T14:20:00Z\"",
                "\"email\":\"lucia.fernandez@ejemplo.pe\"");
    }

    @Test
    void userRegisteredCarriesTheConfirmationLinkAsOptionalFields() {
        adapter.append(UserRegistered.of(user(null), null, TOKEN, REGISTERED_AT.plusSeconds(86_400),
                UUID.randomUUID(), REGISTERED_AT));

        EventContract.assertSameShape(written().getPayload(), "UserRegistered");
        assertThat(written().getPayload()).contains("\"confirmationToken\":\"" + TOKEN + "\"",
                "\"confirmationExpiresAt\":\"2026-10-28T14:00:00Z\"");
    }

    private static User user(Instant emailConfirmedAt) {
        return new User(2001L, User.register("Lucía Fernández Ríos", new Email("lucia.fernandez@ejemplo.pe"),
                "987123456", "hash", Role.CONSUMER), emailConfirmedAt, 0, null, REGISTERED_AT);
    }

    private OutboxEventJpaEntity written() {
        var saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }
}

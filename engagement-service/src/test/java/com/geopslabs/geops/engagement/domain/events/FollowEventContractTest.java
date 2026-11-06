package com.geopslabs.geops.engagement.domain.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geopslabs.geops.engagement.infrastructure.persistence.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FollowEventContractTest {
    @Test
    void bothEventsMatchThePublishedEnvelope() throws Exception {
        var mapper = new ObjectMapper().findAndRegisterModules().disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var repository = mock(OutboxEventJpaRepository.class);
        var clock = Clock.systemUTC();
        var adapter = new OutboxJpaAdapter(repository, mapper, clock);
        for (var event : new DomainEvent[]{BusinessFollowed.of(2001L, 84L, clock.instant()), BusinessUnfollowed.of(2001L, 84L, clock.instant())}) {
            adapter.append(event);
            var captor = org.mockito.ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
            verify(repository).save(captor.capture());
            var actual = mapper.readTree(captor.getValue().getPayload());
            try (var stream = getClass().getResourceAsStream("/events/" + event.eventType() + ".v1.json")) {
                var expected = mapper.readTree(stream);
                assertThat(actual.propertyStream().map(java.util.Map.Entry::getKey)).containsExactlyInAnyOrderElementsOf(expected.propertyStream().map(java.util.Map.Entry::getKey).toList());
                assertThat(actual.get("payload")).isEqualTo(mapper.valueToTree(event.payload()));
                assertThat(actual.get("version").asInt()).isEqualTo(expected.get("version").asInt());
                assertThat(actual.get("eventType")).isEqualTo(expected.get("eventType"));
                assertThat(actual.get("payload").propertyStream().map(java.util.Map.Entry::getKey)).containsExactlyInAnyOrderElementsOf(expected.get("payload").propertyStream().map(java.util.Map.Entry::getKey).toList());
            }
            reset(repository);
        }
    }
}

package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.*;
import com.geopslabs.geops.engagement.domain.models.commands.*;
import com.geopslabs.geops.engagement.domain.models.exceptions.BusinessNotFoundException;
import com.geopslabs.geops.engagement.domain.events.*;
import com.geopslabs.geops.engagement.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FollowCommandServiceTest {
    private final FollowRepositoryPort follows = mock(FollowRepositoryPort.class);
    private final BusinessSnapshotRepositoryPort businesses = mock(BusinessSnapshotRepositoryPort.class);
    private final OutboxPort outbox = mock(OutboxPort.class);
    private final FollowQueryService queries = mock(FollowQueryService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-27T16:00:00Z"), ZoneOffset.UTC);
    private final FollowCommandService service = new FollowCommandService(follows, businesses, outbox, queries, clock);
    private final Follow follow = new Follow(1L, 2001L, 84L, clock.instant());

    @BeforeEach
    void businessExists() {
        when(businesses.findById(84L)).thenReturn(Optional.of(new BusinessSnapshot(84L, "Don Pepe")));
    }

    @Test
    void aNewFollowPublishesOneEvent() {
        when(follows.saveIfAbsent(any())).thenReturn(Optional.of(follow));
        assertThat(service.follow(new FollowBusinessCommand(2001L, 84L)).created()).isTrue();
        verify(outbox).append(argThat(e -> e instanceof BusinessFollowed && e.aggregateId().equals(2001L)));
    }

    @Test
    void aRepeatedFollowDoesNotPublish() {
        when(follows.saveIfAbsent(any())).thenReturn(Optional.empty());
        when(follows.findByConsumerAndBusiness(2001L, 84L)).thenReturn(Optional.of(follow));
        assertThat(service.follow(new FollowBusinessCommand(2001L, 84L)).created()).isFalse();
        verifyNoInteractions(outbox);
    }

    @Test
    void unfollowPublishesOnlyForAnExistingFollow() {
        when(follows.delete(2001L, 84L)).thenReturn(1L, 0L);
        service.unfollow(new UnfollowBusinessCommand(2001L, 84L));
        service.unfollow(new UnfollowBusinessCommand(2001L, 84L));
        verify(outbox, times(1)).append(isA(BusinessUnfollowed.class));
    }

    @Test
    void missingBusinessCannotBeFollowed() {
        assertThatThrownBy(() -> service.follow(new FollowBusinessCommand(2001L, 99L)))
                .isInstanceOf(BusinessNotFoundException.class);
        verifyNoInteractions(follows, outbox);
    }
}

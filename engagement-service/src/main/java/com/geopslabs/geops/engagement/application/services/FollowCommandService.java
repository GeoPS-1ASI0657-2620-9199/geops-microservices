package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.*;
import com.geopslabs.geops.engagement.domain.events.*;
import com.geopslabs.geops.engagement.domain.models.Follow;
import com.geopslabs.geops.engagement.domain.models.commands.*;
import com.geopslabs.geops.engagement.domain.models.exceptions.BusinessNotFoundException;
import com.geopslabs.geops.engagement.domain.ports.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

public class FollowCommandService implements FollowBusinessUseCase, UnfollowBusinessUseCase {
    private final FollowRepositoryPort follows;
    private final BusinessSnapshotRepositoryPort businesses;
    private final OutboxPort outbox;
    private final FollowQueryService queries;
    private final Clock clock;

    public FollowCommandService(FollowRepositoryPort follows, BusinessSnapshotRepositoryPort businesses,
                                OutboxPort outbox, FollowQueryService queries, Clock clock) {
        this.follows = follows;
        this.businesses = businesses;
        this.outbox = outbox;
        this.queries = queries;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FollowBusinessResult follow(FollowBusinessCommand command) {
        businesses.findById(command.businessId()).orElseThrow(BusinessNotFoundException::new);
        var inserted = follows.saveIfAbsent(Follow.start(command.consumerId(), command.businessId(), clock.instant()));
        inserted.ifPresent(follow -> outbox.append(BusinessFollowed.of(follow.consumerId(), follow.businessId(), follow.followedAt())));
        var follow = inserted.orElseGet(() -> follows.findByConsumerAndBusiness(command.consumerId(), command.businessId()).orElseThrow());
        return new FollowBusinessResult(queries.view(follow), inserted.isPresent());
    }

    @Override
    @Transactional
    public void unfollow(UnfollowBusinessCommand command) {
        if (follows.delete(command.consumerId(), command.businessId()) > 0) {
            outbox.append(BusinessUnfollowed.of(command.consumerId(), command.businessId(), clock.instant()));
        }
    }
}

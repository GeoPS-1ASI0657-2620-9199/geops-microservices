package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.Follow;
import java.util.List;
import java.util.Optional;

public interface FollowRepositoryPort {
    Optional<Follow> findByConsumerAndBusiness(Long consumerId, Long businessId);
    Optional<Follow> saveIfAbsent(Follow follow);
    long delete(Long consumerId, Long businessId);
    List<Follow> findByConsumer(Long consumerId);
}

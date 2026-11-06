package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.SavedBusiness;
import java.time.Instant;
import java.util.*;
public interface SavedBusinessRepositoryPort {
    List<SavedBusiness> findByConsumer(Long consumerId);
    Optional<SavedBusiness> saveIfAbsent(Long consumerId, Long businessId, Instant now);
    Optional<SavedBusiness> findByConsumerAndBusiness(Long consumerId, Long businessId);
    long delete(Long consumerId, Long businessId);
}

package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.SavedBusiness;
import java.time.Instant;
import java.util.*;
public interface SavedBusinessRepositoryPort {
    List<SavedBusiness> findByConsumer(Long consumerId);
    SavedBusiness save(Long consumerId, Long businessId, Instant now);
    boolean exists(Long consumerId, Long businessId);
    long delete(Long consumerId, Long businessId);
}

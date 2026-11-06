package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.PushSubscription;
import java.util.*;
public interface PushSubscriptionRepositoryPort {
    Optional<PushSubscription> saveIfAbsent(PushSubscription subscription);
    Optional<PushSubscription> findByToken(String token);
    List<PushSubscription> findByConsumer(Long consumerId);
    void delete(Long id, Long consumerId);
}

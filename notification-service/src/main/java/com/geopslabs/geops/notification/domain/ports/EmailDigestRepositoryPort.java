package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.*;
import java.time.Instant;
import java.util.*;
public interface EmailDigestRepositoryPort {
    void add(Long consumerId, Long offerId, Instant start, Instant end);
    List<EmailDigest> due(Instant now);
    List<Notification> notices(EmailDigest digest);
    void complete(Long digestId, Instant now);
    void removeOffers(Collection<Long> offerIds);
    void withSchedulerLock(Runnable action);
}

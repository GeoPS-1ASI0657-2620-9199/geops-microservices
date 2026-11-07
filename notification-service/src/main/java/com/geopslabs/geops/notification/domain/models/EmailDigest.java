package com.geopslabs.geops.notification.domain.models;

import java.time.Instant;
import java.util.List;
public record EmailDigest(Long id, Long consumerId, Instant windowEnd, List<Long> offerIds) {}

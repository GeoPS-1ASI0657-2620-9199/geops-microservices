package com.geopslabs.geops.engagement.domain.models;

import java.time.Instant;
public record SavedBusiness(Long id, Long consumerId, Long businessId, String businessName, Instant savedAt) {}

package com.geopslabs.geops.engagement.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FollowRequest(@NotNull @Positive Long businessId) {}

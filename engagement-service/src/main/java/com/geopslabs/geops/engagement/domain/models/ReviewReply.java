package com.geopslabs.geops.engagement.domain.models;

import java.time.Instant;
public record ReviewReply(Long id, String text, Instant repliedAt) {}

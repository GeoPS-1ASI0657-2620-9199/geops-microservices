package com.geopslabs.geops.engagement.domain.models.commands;

public record CreateReviewCommand(Long consumerId, String authorFullName, Long businessId, Integer rating, String text) {
}

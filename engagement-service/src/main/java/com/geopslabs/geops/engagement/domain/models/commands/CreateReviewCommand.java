package com.geopslabs.geops.engagement.domain.models.commands;

public record CreateReviewCommand(Long consumerId, Long businessId, Integer rating, String text) {
}

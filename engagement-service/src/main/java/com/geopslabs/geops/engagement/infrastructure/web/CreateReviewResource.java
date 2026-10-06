package com.geopslabs.geops.engagement.infrastructure.web;

public record CreateReviewResource(Long reservationId, Long businessId, Integer rating, String text) {
}

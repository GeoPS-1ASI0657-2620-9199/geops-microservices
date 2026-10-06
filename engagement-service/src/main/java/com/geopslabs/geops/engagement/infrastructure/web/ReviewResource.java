package com.geopslabs.geops.engagement.infrastructure.web;

public record ReviewResource(
    Long id,
    Long reservationId,
    Long consumerId,
    Long businessId,
    Integer rating,
    String text,
    Boolean verifiedRedemption,
    String createdAt
) {

}

package com.geopslabs.geops.engagement.domain.models;

import java.time.LocalDateTime;

public record RedeemedReservation(Long reservationId, Long consumerId, Long businessId, LocalDateTime redeemedAt) {
}

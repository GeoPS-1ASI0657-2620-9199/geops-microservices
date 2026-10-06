package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.Review;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class ReviewPersistenceMapper {

    private ReviewPersistenceMapper() {
    }

    static Review toDomain(ReviewJpaEntity entity) {
        return new Review(entity.getId(), entity.getReservationId(), entity.getConsumerId(), entity.getBusinessId(),
                entity.getRating(), entity.getText(), entity.getVerifiedRedemption(), toUtc(entity.getCreatedAt()));
    }

    static ReviewJpaEntity toEntity(Review review) {
        var entity = new ReviewJpaEntity();
        entity.setId(review.getId());
        entity.setReservationId(review.getReservationId());
        entity.setConsumerId(review.getConsumerId());
        entity.setBusinessId(review.getBusinessId());
        entity.setRating(review.getRating());
        entity.setText(review.getText());
        entity.setVerifiedRedemption(review.getVerifiedRedemption());
        entity.setCreatedAt(toInstant(review.getCreatedAt()));
        return entity;
    }

    private static LocalDateTime toUtc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}

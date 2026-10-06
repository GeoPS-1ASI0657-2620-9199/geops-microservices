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
                entity.getUserName(), entity.getRating(), entity.getText(), entity.getLikes(),
                toUtc(entity.getCreatedAt()));
    }

    static ReviewJpaEntity toEntity(Review review, ReviewJpaEntity entity) {
        entity.setReservationId(review.getReservationId());
        entity.setConsumerId(review.getConsumerId());
        entity.setBusinessId(review.getBusinessId());
        entity.setUserName(review.getUserName());
        entity.setRating(review.getRating());
        entity.setText(review.getText());
        entity.setLikes(review.getLikes());
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

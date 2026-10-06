package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.Rating;
import com.geopslabs.geops.engagement.domain.models.Review;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class ReviewPersistenceMapper {

    private ReviewPersistenceMapper() {
    }

    static Review toDomain(ReviewJpaEntity entity) {
        return new Review(entity.getId(), entity.getConsumerId(), entity.getBusinessId(), entity.getReservationId(),
                new Rating(entity.getRating()), entity.getText(), entity.getVerifiedRedemption(),
                LocalDateTime.ofInstant(entity.getCreatedAt(), ZoneOffset.UTC));
    }

    static ReviewJpaEntity toEntity(Review review) {
        var entity = new ReviewJpaEntity();
        entity.setId(review.getId());
        entity.setReservationId(review.getReservationId());
        entity.setConsumerId(review.getConsumerId());
        entity.setBusinessId(review.getBusinessId());
        entity.setRating(review.getRating().stars());
        entity.setText(review.getText());
        entity.setVerifiedRedemption(review.getVerifiedRedemption());
        entity.setCreatedAt(review.getCreatedAt().toInstant(ZoneOffset.UTC));
        return entity;
    }
}

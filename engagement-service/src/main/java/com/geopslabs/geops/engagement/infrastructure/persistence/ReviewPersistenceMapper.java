package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.Review;

final class ReviewPersistenceMapper {

    private ReviewPersistenceMapper() {
    }

    static Review toDomain(ReviewJpaEntity entity) {
        return new Review(entity.getId(), entity.getOffer(), entity.getUser(), entity.getUserName(),
                entity.getRating(), entity.getText(), entity.getLikes(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static ReviewJpaEntity toEntity(Review review, ReviewJpaEntity entity) {
        entity.setOffer(review.getOffer());
        entity.setUser(review.getUser());
        entity.setUserName(review.getUserName());
        entity.setRating(review.getRating());
        entity.setText(review.getText());
        entity.setLikes(review.getLikes());
        return entity;
    }
}

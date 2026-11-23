package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.Rating;
import com.geopslabs.geops.engagement.domain.models.Review;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewResponseAssemblerTest {
    private static final LocalDateTime CREATED_AT = LocalDateTime.parse("2026-10-08T13:05:00");

    @Test
    void exposesTheShortNameOfTheAuthor() {
        var response = ReviewResponseAssembler.toResponse(review("Lucía F."));

        assertThat(response.authorName()).isEqualTo("Lucía F.");
        assertThat(response.reviewId()).isEqualTo(3L);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-10-08T13:05:00Z"));
    }

    @Test
    void anOlderReviewHasNoAuthorName() {
        assertThat(ReviewResponseAssembler.toResponse(review(null)).authorName()).isNull();
    }

    private static Review review(String authorName) {
        return new Review(3L, 2001L, 84L, 5012L, new Rating(5), "Respetaron el precio.", true, CREATED_AT,
                authorName);
    }
}

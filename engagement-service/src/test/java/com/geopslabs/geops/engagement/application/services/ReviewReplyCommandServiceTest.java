package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.*;
import com.geopslabs.geops.engagement.domain.models.exceptions.*;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewReplyCommandServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
    private final ReviewRepositoryPort reviews = mock(ReviewRepositoryPort.class);
    private final ReviewReplyCommandService service = new ReviewReplyCommandService(reviews, clock);
    private final Review review = new Review(3L, 2001L, 84L, 5012L, new Rating(5), "Good", true, LocalDateTime.now(clock));

    @Test
    void ownerCanReply() {
        when(reviews.findById(3L)).thenReturn(Optional.of(review));
        when(reviews.saveReply(eq(3L), eq(84L), any())).thenAnswer(i -> i.getArgument(2));
        assertThat(service.reply(3L, 84L, "Thanks").text()).isEqualTo("Thanks");
    }

    @Test
    void anotherBusinessCannotReply() {
        when(reviews.findById(3L)).thenReturn(Optional.of(review));
        assertThatThrownBy(() -> service.reply(3L, 99L, "Wrong")).isInstanceOf(ReviewOwnershipException.class);
        verify(reviews, never()).saveReply(any(), any(), any());
    }

    @Test
    void aSecondReplyIsRejected() {
        review.withReply(new ReviewReply(1L, "Thanks", clock.instant()));
        when(reviews.findById(3L)).thenReturn(Optional.of(review));
        assertThatThrownBy(() -> service.reply(3L, 84L, "Again")).isInstanceOf(ReplyAlreadyExistsException.class);
    }

    @Test
    void missingReviewReturnsNotFound() {
        assertThatThrownBy(() -> service.reply(3L, 84L, "Thanks")).isInstanceOf(ReviewNotFoundException.class);
    }
}

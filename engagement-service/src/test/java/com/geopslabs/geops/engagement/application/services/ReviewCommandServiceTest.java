package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.RedeemedReservation;
import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;
import com.geopslabs.geops.engagement.domain.models.exceptions.RedemptionRequiredException;
import com.geopslabs.geops.engagement.domain.models.exceptions.ReviewAlreadyExistsException;
import com.geopslabs.geops.engagement.domain.ports.RedeemedReservationRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-08T13:05:00Z");
    private static final Long CONSUMER_ID = 1L;
    private static final Long BUSINESS_ID = 1L;
    private static final Long OLDEST_RESERVATION_ID = 11L;
    private static final Long REVIEW_ID = 3L;
    private static final int FIVE_STARS = 5;
    private static final String TEXT = "Respetaron el precio";
    private static final CreateReviewCommand COMMAND = new CreateReviewCommand(CONSUMER_ID, BUSINESS_ID, FIVE_STARS,
            TEXT);
    private static final RedeemedReservation OLDEST_REDEMPTION = new RedeemedReservation(OLDEST_RESERVATION_ID,
            CONSUMER_ID, BUSINESS_ID, LocalDateTime.parse("2026-10-01T18:00:00"));

    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private RedeemedReservationRepositoryPort redeemedReservationRepository;

    private ReviewCommandService service;

    @BeforeEach
    void setUp() {
        service = new ReviewCommandService(reviewRepository, redeemedReservationRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsAVerifiedReviewForTheBusinessOfTheRedemption() {
        givenUnreviewedRedemption();
        when(reviewRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        var review = service.create(COMMAND);

        assertThat(review.getId()).isEqualTo(REVIEW_ID);
        assertThat(review.getVerifiedRedemption()).isTrue();
        assertThat(review.getBusinessId()).isEqualTo(BUSINESS_ID);
        assertThat(review.getConsumerId()).isEqualTo(CONSUMER_ID);
        assertThat(review.getRating().stars()).isEqualTo(FIVE_STARS);
        assertThat(review.getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }

    @Test
    void usesTheOldestRedemptionWithoutReview() {
        givenUnreviewedRedemption();
        when(reviewRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        service.create(COMMAND);

        var saved = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(saved.capture());
        assertThat(saved.getValue().getReservationId()).isEqualTo(OLDEST_RESERVATION_ID);
    }

    @Test
    void aConsumerWithoutARedemptionInThatBusinessCannotReview() {
        when(redeemedReservationRepository.findUnreviewed(CONSUMER_ID, BUSINESS_ID)).thenReturn(Optional.empty());
        when(redeemedReservationRepository.existsFor(CONSUMER_ID, BUSINESS_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(COMMAND)).isInstanceOf(RedemptionRequiredException.class);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void aConsumerWhoseRedemptionsAreAllReviewedCannotReviewAgain() {
        when(redeemedReservationRepository.findUnreviewed(CONSUMER_ID, BUSINESS_ID)).thenReturn(Optional.empty());
        when(redeemedReservationRepository.existsFor(CONSUMER_ID, BUSINESS_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(COMMAND)).isInstanceOf(ReviewAlreadyExistsException.class);
        verify(reviewRepository, never()).save(any());
    }

    private void givenUnreviewedRedemption() {
        when(redeemedReservationRepository.findUnreviewed(CONSUMER_ID, BUSINESS_ID))
                .thenReturn(Optional.of(OLDEST_REDEMPTION));
    }

    private static Review withId(Review review) {
        return new Review(REVIEW_ID, review.getConsumerId(), review.getBusinessId(), review.getReservationId(),
                review.getRating(), review.getText(), review.getVerifiedRedemption(), review.getCreatedAt());
    }
}

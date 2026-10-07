package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.CreateReviewUseCase;
import com.geopslabs.geops.engagement.domain.models.Rating;
import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;
import com.geopslabs.geops.engagement.domain.models.exceptions.RedemptionRequiredException;
import com.geopslabs.geops.engagement.domain.models.exceptions.ReviewAlreadyExistsException;
import com.geopslabs.geops.engagement.domain.ports.RedeemedReservationRepositoryPort;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class ReviewCommandService implements CreateReviewUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewCommandService.class);

    private final ReviewRepositoryPort reviewRepository;
    private final RedeemedReservationRepositoryPort redeemedReservationRepository;
    private final Clock clock;

    public ReviewCommandService(ReviewRepositoryPort reviewRepository,
                                RedeemedReservationRepositoryPort redeemedReservationRepository, Clock clock) {
        this.reviewRepository = reviewRepository;
        this.redeemedReservationRepository = redeemedReservationRepository;
        this.clock = clock;
    }

    @Override
    public Review create(CreateReviewCommand command) {
        var redemption = redeemedReservationRepository.findUnreviewed(command.consumerId(), command.businessId())
                .orElseThrow(() -> missingRedemption(command));
        var review = Review.of(redemption, new Rating(command.rating()), command.text(), now());
        var saved = reviewRepository.save(review);
        LOGGER.info("review.created reviewId={} businessId={}", saved.getId(), saved.getBusinessId());
        return saved;
    }

    private RuntimeException missingRedemption(CreateReviewCommand command) {
        if (redeemedReservationRepository.existsFor(command.consumerId(), command.businessId())) {
            LOGGER.info("review.rejected businessId={} reason=every-redemption-reviewed", command.businessId());
            return new ReviewAlreadyExistsException();
        }
        LOGGER.info("review.rejected businessId={} reason=no-redemption", command.businessId());
        return new RedemptionRequiredException();
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}

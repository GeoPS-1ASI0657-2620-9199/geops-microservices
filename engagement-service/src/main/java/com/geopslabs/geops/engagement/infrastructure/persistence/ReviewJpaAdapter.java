package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.models.exceptions.ReviewAlreadyExistsException;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReviewJpaAdapter implements ReviewRepositoryPort {
    static final String REVIEW_RESERVATION_UNIQUE_KEY = "uk_reviews_reservation_id";

    private final ReviewJpaRepository repository;

    public ReviewJpaAdapter(ReviewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Review save(Review review) {
        try {
            return ReviewPersistenceMapper.toDomain(repository.saveAndFlush(ReviewPersistenceMapper.toEntity(review)));
        } catch (DataIntegrityViolationException exception) {
            throw translate(exception);
        }
    }

    @Override
    public List<Review> findByBusinessId(Long businessId) {
        return repository.findByBusinessIdOrderByCreatedAtDescIdDesc(businessId).stream()
                .map(ReviewPersistenceMapper::toDomain)
                .toList();
    }

    private static RuntimeException translate(DataIntegrityViolationException exception) {
        var cause = NestedExceptionUtils.getMostSpecificCause(exception).getMessage();
        if (cause != null && cause.contains(REVIEW_RESERVATION_UNIQUE_KEY)) {
            return new ReviewAlreadyExistsException();
        }
        return exception;
    }
}

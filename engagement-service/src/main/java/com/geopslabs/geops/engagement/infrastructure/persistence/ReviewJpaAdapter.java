package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReviewJpaAdapter implements ReviewRepositoryPort {
    private final ReviewJpaRepository repository;

    public ReviewJpaAdapter(ReviewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Review save(Review review) {
        return ReviewPersistenceMapper.toDomain(repository.saveAndFlush(ReviewPersistenceMapper.toEntity(review)));
    }

    @Override
    public List<Review> findByBusinessIdOrderByCreatedAtDesc(Long businessId) {
        return repository.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(ReviewPersistenceMapper::toDomain)
                .toList();
    }
}

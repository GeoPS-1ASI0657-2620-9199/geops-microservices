package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.Review;
import com.geopslabs.geops.engagement.domain.ports.ReviewRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ReviewJpaAdapter implements ReviewRepositoryPort {
    private final ReviewJpaRepository repository;

    public ReviewJpaAdapter(ReviewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Review save(Review review) {
        var entity = review.getId() == null ? new ReviewJpaEntity()
                : repository.findById(review.getId()).orElseGet(ReviewJpaEntity::new);
        return ReviewPersistenceMapper.toDomain(repository.save(ReviewPersistenceMapper.toEntity(review, entity)));
    }

    @Override
    public Optional<Review> findById(Long id) {
        return repository.findById(id).map(ReviewPersistenceMapper::toDomain);
    }

    @Override
    public List<Review> findAll() {
        return toDomain(repository.findAll());
    }

    @Override
    public List<Review> findByOfferIdOrderByCreatedAtDesc(Long offerId) {
        return toDomain(repository.findByOffer_IdOrderByCreatedAtDesc(offerId));
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private static List<Review> toDomain(List<ReviewJpaEntity> entities) {
        return entities.stream().map(ReviewPersistenceMapper::toDomain).toList();
    }
}

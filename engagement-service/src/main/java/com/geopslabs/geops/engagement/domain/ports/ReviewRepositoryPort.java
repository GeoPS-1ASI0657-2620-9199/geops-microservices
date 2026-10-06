package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepositoryPort {
    Review save(Review review);

    Optional<Review> findById(Long id);

    List<Review> findAll();

    List<Review> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    boolean existsById(Long id);

    void deleteById(Long id);
}

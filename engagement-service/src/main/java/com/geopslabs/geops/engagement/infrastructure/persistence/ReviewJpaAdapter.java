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
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public ReviewJpaAdapter(ReviewJpaRepository repository, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
    }

    @Override
    public java.util.Optional<Review> findById(Long reviewId) {
        return repository.findById(reviewId).map(ReviewPersistenceMapper::toDomain).map(this::withReply);
    }

    @Override
    public com.geopslabs.geops.engagement.domain.models.ReviewReply saveReply(Long reviewId, Long businessId, com.geopslabs.geops.engagement.domain.models.ReviewReply reply) {
        var replies = jdbc.query("""
                INSERT INTO review_replies (review_id, business_id, reply_text, replied_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (review_id) DO NOTHING RETURNING id, reply_text, replied_at""",
                (rs, index) -> new com.geopslabs.geops.engagement.domain.models.ReviewReply(rs.getLong("id"), rs.getString("reply_text"), rs.getTimestamp("replied_at").toInstant()),
                reviewId, businessId, reply.text(), java.sql.Timestamp.from(reply.repliedAt()));
        return replies.stream().findFirst().orElseThrow(com.geopslabs.geops.engagement.domain.models.exceptions.ReplyAlreadyExistsException::new);
    }

    private Review withReply(Review review) {
        var reply = jdbc.query("SELECT id, reply_text, replied_at FROM review_replies WHERE review_id = ?",
                (rs, index) -> new com.geopslabs.geops.engagement.domain.models.ReviewReply(rs.getLong("id"), rs.getString("reply_text"), rs.getTimestamp("replied_at").toInstant()), review.getId());
        return review.withReply(reply.stream().findFirst().orElse(null));
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
                .map(this::withReply)
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

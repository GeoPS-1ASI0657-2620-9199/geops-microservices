package com.geopslabs.geops.engagement.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "reviews")
@Getter
@Setter
public class ReviewJpaEntity {
    private static final int REVIEW_TEXT_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private Long reservationId;

    @Column(name = "consumer_id", nullable = false)
    private Long consumerId;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "review_text", nullable = false, length = REVIEW_TEXT_LENGTH)
    private String text;

    @Column(name = "verified_redemption", nullable = false)
    private Boolean verifiedRedemption;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

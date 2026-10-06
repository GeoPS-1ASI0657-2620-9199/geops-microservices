package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.backend.identity.domain.model.aggregates.User;
import com.geopslabs.geops.backend.offers.domain.model.aggregates.Offer;
import com.geopslabs.geops.backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_offer_id", columnList = "offer_id"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
public class ReviewJpaEntity extends AuditableAbstractAggregateRoot<ReviewJpaEntity> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "text", nullable = false, length = 2000)
    private String text;

    @Column(name = "likes", nullable = false)
    private Integer likes;
}

package com.geopslabs.geops.engagement.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "saved_offers")
@Getter
@Setter
public class SavedOfferJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_id", nullable = false)
    private Long consumerId;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    @PrePersist
    void stampSavedAt() {
        if (savedAt == null) {
            savedAt = Instant.now();
        }
    }
}

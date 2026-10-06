package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.shared.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "offers")
@Getter
@Setter
public class OfferJpaEntity extends AuditableAbstractAggregateRoot<OfferJpaEntity> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private CampaignJpaEntity campaign;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "partner", nullable = false, length = 150)
    private String partner;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "code_prefix", nullable = false, length = 10)
    private String codePrefix;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "image_url", length = 255)
    private String imageUrl;
}

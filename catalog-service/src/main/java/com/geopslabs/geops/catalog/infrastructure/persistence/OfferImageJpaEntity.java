package com.geopslabs.geops.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offer_images")
@Getter
@Setter
public class OfferImageJpaEntity {
    private static final int CONTENT_TYPE_LENGTH = 30;

    @Id
    @Column(name = "image_id", nullable = false)
    private UUID imageId;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "content_type", nullable = false, length = CONTENT_TYPE_LENGTH)
    private String contentType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
}

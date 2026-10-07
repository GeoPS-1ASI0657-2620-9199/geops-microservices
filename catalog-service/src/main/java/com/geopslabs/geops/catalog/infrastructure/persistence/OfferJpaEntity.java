package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.OfferSource;
import com.geopslabs.geops.catalog.domain.models.OfferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "offers")
@Getter
@Setter
public class OfferJpaEntity {
    private static final int TITLE_LENGTH = 255;
    private static final int CATEGORY_LENGTH = 100;
    private static final int STATUS_LENGTH = 20;
    private static final int SOURCE_LENGTH = 20;
    private static final int ADDRESS_LENGTH = 255;
    private static final int IMAGE_URL_LENGTH = 500;
    private static final int SOURCE_NAME_LENGTH = 150;
    private static final int AMOUNT_PRECISION = 10;
    private static final int AMOUNT_SCALE = 2;
    private static final String TEXT = "text";
    private static final String GEOGRAPHY_POINT = "geography(Point,4326)";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "title", nullable = false, length = TITLE_LENGTH)
    private String title;

    @Column(name = "conditions", nullable = false, columnDefinition = TEXT)
    private String conditions;

    @Column(name = "price", nullable = false, precision = AMOUNT_PRECISION, scale = AMOUNT_SCALE)
    private BigDecimal price;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "category", nullable = false, length = CATEGORY_LENGTH)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "geocoding_status", nullable = false, length = STATUS_LENGTH)
    private GeocodingStatus geocodingStatus;

    @Column(name = "address", nullable = false, length = ADDRESS_LENGTH)
    private String address;

    @Column(name = "image_url", length = IMAGE_URL_LENGTH)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = SOURCE_LENGTH)
    private OfferSource source;

    @Column(name = "source_name", length = SOURCE_NAME_LENGTH)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = STATUS_LENGTH)
    private OfferStatus status;

    @Column(name = "location", columnDefinition = GEOGRAPHY_POINT)
    private Point location;
}

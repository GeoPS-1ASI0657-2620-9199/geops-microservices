package com.geopslabs.geops.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(name = "last_known_locations")
@Getter
@Setter
public class LastKnownLocationJpaEntity {
    static final String GEOGRAPHY_POINT = "geography(Point,4326)";

    @Id
    @Column(name = "consumer_id")
    private Long consumerId;

    @Column(name = "position", nullable = false, columnDefinition = GEOGRAPHY_POINT)
    private Point position;

    @Column(name = "accuracy_m", nullable = false)
    private Integer accuracyMeters;

    @Column(name = "radius_m", nullable = false)
    private Integer radiusMeters;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}

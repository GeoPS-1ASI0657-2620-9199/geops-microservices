package com.geopslabs.geops.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "consumer_profiles")
@Getter
@Setter
@NoArgsConstructor
public class ConsumerProfileJpaEntity {
    private static final int DEFAULT_DISTRICT_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "location_permission", nullable = false)
    private Boolean locationPermission;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "search_radius_minutes", nullable = false)
    private Integer searchRadiusMinutes;

    @Column(name = "default_district", length = DEFAULT_DISTRICT_LENGTH)
    private String defaultDistrict;
}

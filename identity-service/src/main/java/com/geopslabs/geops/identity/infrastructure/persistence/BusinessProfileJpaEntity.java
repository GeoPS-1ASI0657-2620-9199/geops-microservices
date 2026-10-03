package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.AccountStatus;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "business_profiles")
@Getter
@Setter
@NoArgsConstructor
public class BusinessProfileJpaEntity {
    private static final int BUSINESS_NAME_LENGTH = 150;
    private static final int BUSINESS_TYPE_LENGTH = 100;
    private static final int RUC_LENGTH = 11;
    private static final int ADDRESS_LENGTH = 255;
    private static final int COORDINATE_PRECISION = 9;
    private static final int COORDINATE_SCALE = 6;
    private static final int OPENING_HOURS_LENGTH = 255;
    private static final int STATUS_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "business_name", nullable = false, length = BUSINESS_NAME_LENGTH)
    private String businessName;

    @Column(name = "business_type", length = BUSINESS_TYPE_LENGTH)
    private String businessType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ruc", nullable = false, unique = true, length = RUC_LENGTH)
    private String ruc;

    @Column(name = "address", nullable = false, length = ADDRESS_LENGTH)
    private String address;

    @Column(name = "latitude", precision = COORDINATE_PRECISION, scale = COORDINATE_SCALE)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = COORDINATE_PRECISION, scale = COORDINATE_SCALE)
    private BigDecimal longitude;

    @Column(name = "opening_hours", length = OPENING_HOURS_LENGTH)
    private String openingHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = STATUS_LENGTH)
    private AccountStatus accountStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = STATUS_LENGTH)
    private VerificationStatus verificationStatus;
}

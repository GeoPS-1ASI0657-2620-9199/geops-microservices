package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.shared.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "details_owner")
@Getter
public class BusinessProfileJpaEntity extends AuditableAbstractAggregateRoot<BusinessProfileJpaEntity> {
    private static final int BUSINESS_TYPE_LENGTH = 100;
    private static final int TAX_ID_LENGTH = 50;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "business_type", length = BUSINESS_TYPE_LENGTH)
    private String businessType;

    @Column(name = "tax_id", length = TAX_ID_LENGTH)
    private String taxId;

    @Column(name = "website")
    private String website;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address")
    private String address;

    @Column(name = "horario_atencion")
    private String horarioAtencion;

    protected BusinessProfileJpaEntity() {
    }

    public BusinessProfileJpaEntity(UserJpaEntity user) {
        this.user = user;
    }

    public void update(String businessName, String businessType, String taxId, String website,
                       String description, String address, String horarioAtencion) {
        this.businessName = businessName;
        this.businessType = businessType;
        this.taxId = taxId;
        this.website = website;
        this.description = description;
        this.address = address;
        this.horarioAtencion = horarioAtencion;
    }
}

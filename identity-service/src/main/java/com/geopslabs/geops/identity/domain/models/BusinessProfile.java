package com.geopslabs.geops.identity.domain.models;

import java.util.Date;

public class BusinessProfile {
    private Long id;
    private User user;
    private String businessName;
    private String businessType;
    private String taxId;
    private String website;
    private String description;
    private String address;
    private String horarioAtencion;
    private Date createdAt;
    private Date updatedAt;

    public BusinessProfile(User user, String businessName, String businessType, String taxId,
                           String website, String description, String address, String horarioAtencion) {
        this.user = user;
        this.businessName = businessName;
        this.businessType = businessType;
        this.taxId = taxId;
        this.website = website;
        this.description = description;
        this.address = address;
        this.horarioAtencion = horarioAtencion;
    }

    public BusinessProfile(Long id, BusinessProfile data, Date createdAt, Date updatedAt) {
        this(data.user, data.businessName, data.businessType, data.taxId, data.website,
                data.description, data.address, data.horarioAtencion);
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateOwnerDetails(String businessName, String businessType, String taxId,
                                   String website, String description, String address,
                                   String horarioAtencion) {
        this.businessName = businessName != null && !businessName.isBlank() ? businessName : this.businessName;
        this.businessType = valueOrCurrent(businessType, this.businessType);
        this.taxId = valueOrCurrent(taxId, this.taxId);
        this.website = valueOrCurrent(website, this.website);
        this.description = valueOrCurrent(description, this.description);
        this.address = valueOrCurrent(address, this.address);
        this.horarioAtencion = valueOrCurrent(horarioAtencion, this.horarioAtencion);
    }

    private static String valueOrCurrent(String candidate, String current) {
        return candidate != null ? candidate : current;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getWebsite() {
        return website;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public String getHorarioAtencion() {
        return horarioAtencion;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}

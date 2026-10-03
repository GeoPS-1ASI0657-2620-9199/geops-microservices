package com.geopslabs.geops.identity.domain.models;

public class BusinessProfile {
    private Long id;
    private Long userId;
    private String businessName;
    private String businessType;
    private String ruc;
    private String address;
    private Double latitude;
    private Double longitude;
    private String openingHours;
    private AccountStatus accountStatus;
    private VerificationStatus verificationStatus;

    public BusinessProfile(Long userId, String businessName, String businessType, String ruc, String address,
                           String openingHours) {
        this.userId = userId;
        this.businessName = businessName;
        this.businessType = businessType;
        this.ruc = ruc;
        this.address = address;
        this.openingHours = openingHours;
        this.accountStatus = AccountStatus.ACTIVE;
        this.verificationStatus = VerificationStatus.UNVERIFIED;
    }

    public BusinessProfile(Long id, BusinessProfile data, Double latitude, Double longitude,
                           AccountStatus accountStatus, VerificationStatus verificationStatus) {
        this(data.userId, data.businessName, data.businessType, data.ruc, data.address, data.openingHours);
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accountStatus = accountStatus;
        this.verificationStatus = verificationStatus;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getRuc() {
        return ruc;
    }

    public String getAddress() {
        return address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }
}

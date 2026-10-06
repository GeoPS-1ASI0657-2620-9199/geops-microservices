package com.geopslabs.geops.identity.domain.models;

public class BusinessProfile {
    private Long id;
    private Long userId;
    private String businessName;
    private String businessType;
    private Ruc ruc;
    private String address;
    private GeoPoint location;
    private String openingHours;
    private AccountStatus accountStatus;
    private VerificationStatus verificationStatus;

    public BusinessProfile(String businessName, String businessType, Ruc ruc, String address, GeoPoint location,
                           String openingHours) {
        this.businessName = businessName;
        this.businessType = businessType;
        this.ruc = ruc;
        this.address = address;
        this.location = location;
        this.openingHours = openingHours;
        this.accountStatus = AccountStatus.ACTIVE;
        this.verificationStatus = VerificationStatus.UNVERIFIED;
    }

    public BusinessProfile(Long id, Long userId, BusinessProfile data, AccountStatus accountStatus,
                           VerificationStatus verificationStatus) {
        this(data.businessName, data.businessType, data.ruc, data.address, data.location, data.openingHours);
        this.id = id;
        this.userId = userId;
        this.accountStatus = accountStatus;
        this.verificationStatus = verificationStatus;
    }

    public static BusinessProfile register(String businessName, String businessType, Ruc ruc, String address,
                                           GeoPoint location, String openingHours) {
        ensureRucIsWellFormed(ruc);
        ensureAddressIsPresent(address);
        return new BusinessProfile(businessName, businessType, ruc, address.strip(), location, openingHours);
    }

    public BusinessProfile ownedBy(Long ownerId) {
        return new BusinessProfile(id, ownerId, this, accountStatus, verificationStatus);
    }

    private static void ensureRucIsWellFormed(Ruc ruc) {
        if (!ruc.isWellFormed()) {
            throw new InvalidRucException();
        }
    }

    private static void ensureAddressIsPresent(String address) {
        if (address == null || address.isBlank()) {
            throw InvalidLocationException.missingAddress();
        }
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

    public Ruc getRuc() {
        return ruc;
    }

    public String getAddress() {
        return address;
    }

    public GeoPoint getLocation() {
        return location;
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

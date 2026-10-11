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

    /**
     * Replaces the declared address with the one the owner corrected. The point marked on the map when the business
     * registered (US22) stays as it was: geocoding only checks that the address exists.
     */
    public void correctAddress(String newAddress) {
        ensureAddressIsPresent(newAddress);
        this.address = newAddress.strip();
    }

    /**
     * Applies a verification that checked {@code correctedAddress} instead of the declared one (null when the owner
     * did not correct it). A business that is already verified only takes a new address that was geocoded: if the
     * geocoder could not find it, or did not answer, the previous address and the seal stay and nothing changes.
     * Otherwise the corrected address is kept even when the attempt is rejected, so the owner does not type it again.
     */
    public SealChange applyVerification(RucVerification verification, String correctedAddress) {
        if (correctedAddress != null) {
            var addressNotConfirmed = verification.reason() == VerificationReason.ADDRESS_NOT_GEOCODABLE
                    || verification.reason() == VerificationReason.GEOCODER_UNAVAILABLE;
            if (isVerified() && addressNotConfirmed) {
                return SealChange.UNCHANGED;
            }
            correctAddress(correctedAddress);
        }
        return applyVerification(verification);
    }

    /** The address as it would be stored, or InvalidLocationException when it is blank. */
    public static String normalizedAddress(String address) {
        ensureAddressIsPresent(address);
        return address.strip();
    }

    /**
     * Moves the profile to the status the verification leads to and says what happened to the seal. A verified
     * business keeps its seal while the geocoder does not answer: the seal is never granted without an answer, and
     * it is not taken away for one either.
     */
    public SealChange applyVerification(RucVerification verification) {
        var wasVerified = verificationStatus == VerificationStatus.VERIFIED;
        return switch (verification.result()) {
            case APPROVED -> {
                verificationStatus = VerificationStatus.VERIFIED;
                yield wasVerified ? SealChange.CONFIRMED : SealChange.GRANTED;
            }
            case REJECTED -> {
                verificationStatus = VerificationStatus.REJECTED;
                yield wasVerified ? SealChange.REVOKED : SealChange.UNCHANGED;
            }
            case PENDING -> {
                if (!wasVerified) {
                    verificationStatus = VerificationStatus.PENDING;
                }
                yield SealChange.UNCHANGED;
            }
        };
    }

    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
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

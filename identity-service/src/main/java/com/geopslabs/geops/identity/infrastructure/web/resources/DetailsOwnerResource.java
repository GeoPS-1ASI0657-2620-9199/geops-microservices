package com.geopslabs.geops.identity.infrastructure.web.resources;

public record DetailsOwnerResource(
    Long id,
    Long userId,
    String businessName,
    String businessType,
    String ruc,
    String address,
    Double latitude,
    Double longitude,
    String openingHours,
    String accountStatus,
    String verificationStatus
) {
}

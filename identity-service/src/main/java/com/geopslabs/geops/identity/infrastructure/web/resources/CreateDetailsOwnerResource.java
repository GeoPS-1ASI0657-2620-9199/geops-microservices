package com.geopslabs.geops.identity.infrastructure.web.resources;

public record CreateDetailsOwnerResource(
    String businessName,
    String businessType,
    String ruc,
    String address,
    String openingHours
) {
}

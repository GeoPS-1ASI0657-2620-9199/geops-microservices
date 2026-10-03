package com.geopslabs.geops.identity.infrastructure.web.resources;

public record DetailsConsumerResource(
    Long id,
    Long userId,
    Boolean locationPermission,
    Integer searchRadiusMinutes,
    String defaultDistrict
) {
}

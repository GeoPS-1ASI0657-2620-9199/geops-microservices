package com.geopslabs.geops.identity.infrastructure.web.resources;

public record CreateDetailsConsumerResource(
    Boolean locationPermission,
    Integer searchRadiusMinutes,
    String defaultDistrict
) {
}

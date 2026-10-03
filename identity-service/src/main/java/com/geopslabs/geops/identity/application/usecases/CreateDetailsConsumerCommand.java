package com.geopslabs.geops.identity.application.usecases;

public record CreateDetailsConsumerCommand(
    Long userId,
    Boolean locationPermission,
    Integer searchRadiusMinutes,
    String defaultDistrict
) {
    public CreateDetailsConsumerCommand {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
    }
}

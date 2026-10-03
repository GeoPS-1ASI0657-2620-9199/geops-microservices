package com.geopslabs.geops.identity.application.usecases;

public record UpdateDetailsOwnerCommand(
    Long userId,
    String businessName,
    String businessType,
    String address,
    String openingHours
) {
    public UpdateDetailsOwnerCommand {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
    }
}

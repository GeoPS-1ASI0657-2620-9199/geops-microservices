package com.geopslabs.geops.identity.application.usecases;

public record CreateDetailsOwnerCommand(
    Long userId,
    String businessName,
    String businessType,
    String ruc,
    String address,
    String openingHours
) {
    public CreateDetailsOwnerCommand {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
        if (businessName == null || businessName.isBlank()) {
            throw new IllegalArgumentException("Business name is required");
        }
        if (ruc == null || ruc.isBlank()) {
            throw new IllegalArgumentException("RUC is required");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
    }
}

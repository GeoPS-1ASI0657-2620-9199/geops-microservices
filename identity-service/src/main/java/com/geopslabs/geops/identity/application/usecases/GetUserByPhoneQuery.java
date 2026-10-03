package com.geopslabs.geops.identity.application.usecases;

public record GetUserByPhoneQuery(String phone) {
    public GetUserByPhoneQuery {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone cannot be null or empty");
        }
    }
}

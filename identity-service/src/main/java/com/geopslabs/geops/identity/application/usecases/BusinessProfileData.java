package com.geopslabs.geops.identity.application.usecases;

public record BusinessProfileData(String businessName, String businessType, String ruc, String address,
                                  Double latitude, Double longitude, String openingHours) {
}

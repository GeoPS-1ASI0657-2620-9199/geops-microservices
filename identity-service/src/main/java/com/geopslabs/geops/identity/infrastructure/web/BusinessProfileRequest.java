package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.BusinessProfileData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BusinessProfileRequest(
        @NotBlank @Size(max = BusinessProfileRequest.BUSINESS_NAME_MAX_LENGTH) String businessName,
        @Size(max = BusinessProfileRequest.BUSINESS_TYPE_MAX_LENGTH) String businessType,
        String ruc,
        @Size(max = BusinessProfileRequest.ADDRESS_MAX_LENGTH) String address,
        Double latitude,
        Double longitude,
        @Size(max = BusinessProfileRequest.OPENING_HOURS_MAX_LENGTH) String openingHours) {

    static final int BUSINESS_NAME_MAX_LENGTH = 150;
    static final int BUSINESS_TYPE_MAX_LENGTH = 100;
    static final int ADDRESS_MAX_LENGTH = 255;
    static final int OPENING_HOURS_MAX_LENGTH = 255;

    public BusinessProfileData toData() {
        return new BusinessProfileData(businessName.strip(), businessType, ruc, address, latitude, longitude,
                openingHours);
    }
}

package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.domain.models.ConsumerProfile;

public record ConsumerProfileResponse(Long consumerId, int searchRadiusMinutes, boolean locationPermission,
                                      String defaultDistrict) {

    static ConsumerProfileResponse from(ConsumerProfile profile) {
        return new ConsumerProfileResponse(profile.getUserId(), profile.getSearchRadiusMinutes(),
                profile.isLocationPermission(), profile.getDefaultDistrict());
    }
}

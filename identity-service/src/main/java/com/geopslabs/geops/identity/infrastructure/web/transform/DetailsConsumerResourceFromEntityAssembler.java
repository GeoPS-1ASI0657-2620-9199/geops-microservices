package com.geopslabs.geops.identity.infrastructure.web.transform;

import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.infrastructure.web.resources.DetailsConsumerResource;

public class DetailsConsumerResourceFromEntityAssembler {
    public static DetailsConsumerResource toResourceFromEntity(ConsumerProfile entity) {
        return new DetailsConsumerResource(
            entity.getId(),
            entity.getUserId(),
            entity.isLocationPermission(),
            entity.getSearchRadiusMinutes(),
            entity.getDefaultDistrict()
        );
    }
}

package com.geopslabs.geops.identity.infrastructure.web.transform;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.infrastructure.web.resources.DetailsOwnerResource;

public class DetailsOwnerResourceFromEntityAssembler {
    public static DetailsOwnerResource toResourceFromEntity(BusinessProfile entity) {
        return new DetailsOwnerResource(
            entity.getId(),
            entity.getUserId(),
            entity.getBusinessName(),
            entity.getBusinessType(),
            entity.getRuc(),
            entity.getAddress(),
            entity.getLatitude(),
            entity.getLongitude(),
            entity.getOpeningHours(),
            entity.getAccountStatus().name(),
            entity.getVerificationStatus().name()
        );
    }
}

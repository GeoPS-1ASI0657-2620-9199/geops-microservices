package com.geopslabs.geops.identity.infrastructure.web.transform;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.infrastructure.web.resources.DetailsOwnerResource;

/**
 * DetailsOwnerResourceFromEntityAssembler
 *
 * Assembler class that transforms BusinessProfile entities to DetailsOwnerResource Resources
 * This class follows the Assembler pattern to separate domain objects from REST representations
 *
 * @summary Transforms BusinessProfile entities to resource Resources
 * @since 1.0
 * @author GeOps Labs
 */
public class DetailsOwnerResourceFromEntityAssembler {

    /**
     * Transforms a BusinessProfile entity to a DetailsOwnerResource
     *
     * @param entity The BusinessProfile entity to transform
     * @return The corresponding DetailsOwnerResource Resource
     */
    public static DetailsOwnerResource toResourceFromEntity(BusinessProfile entity) {
        return new DetailsOwnerResource(
            entity.getId(),
            entity.getUser().getId(),
            entity.getBusinessName(),
            entity.getBusinessType(),
            entity.getTaxId(),
            entity.getWebsite(),
            entity.getDescription(),
            entity.getAddress(),
            entity.getHorarioAtencion(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}


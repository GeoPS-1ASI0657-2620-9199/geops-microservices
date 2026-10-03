package com.geopslabs.geops.identity.infrastructure.web.transform;

import com.geopslabs.geops.identity.application.usecases.UpdateUserCommand;
import com.geopslabs.geops.identity.infrastructure.web.resources.UpdateUserResource;

public class UpdateUserCommandFromResourceAssembler {
    public static UpdateUserCommand toCommandFromResource(Long id, UpdateUserResource resource) {
        return new UpdateUserCommand(
            id,
            resource.name(),
            resource.email(),
            resource.phone(),
            resource.role()
        );
    }
}

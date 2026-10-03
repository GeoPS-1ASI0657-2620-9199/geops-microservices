package com.geopslabs.geops.identity.infrastructure.web.transform;

import com.geopslabs.geops.identity.application.usecases.CreateUserCommand;
import com.geopslabs.geops.identity.infrastructure.web.resources.CreateUserResource;

public class CreateUserCommandFromResourceAssembler {
    public static CreateUserCommand toCommandFromResource(CreateUserResource resource) {
        return new CreateUserCommand(
            resource.name(),
            resource.email(),
            resource.password(), resource.phone(),
            resource.role()
        );
    }
}

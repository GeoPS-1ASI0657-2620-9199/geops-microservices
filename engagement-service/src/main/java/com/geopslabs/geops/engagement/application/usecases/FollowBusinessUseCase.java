package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.commands.FollowBusinessCommand;

public interface FollowBusinessUseCase {
    FollowBusinessResult follow(FollowBusinessCommand command);
}

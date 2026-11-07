package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.commands.UnfollowBusinessCommand;

public interface UnfollowBusinessUseCase {
    void unfollow(UnfollowBusinessCommand command);
}

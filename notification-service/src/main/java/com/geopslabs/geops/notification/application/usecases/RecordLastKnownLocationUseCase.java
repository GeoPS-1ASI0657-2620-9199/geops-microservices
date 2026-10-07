package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.commands.RecordLocationCommand;

public interface RecordLastKnownLocationUseCase {
    RecordedLocation record(RecordLocationCommand command);
}

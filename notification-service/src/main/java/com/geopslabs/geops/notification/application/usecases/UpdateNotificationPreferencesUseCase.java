package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.NotificationPreference;
import com.geopslabs.geops.notification.domain.models.commands.UpdatePreferencesCommand;

public interface UpdateNotificationPreferencesUseCase {
    NotificationPreference update(UpdatePreferencesCommand command);
}

package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.NotificationPreference;
public interface GetNotificationPreferencesUseCase { NotificationPreference get(Long consumerId); }

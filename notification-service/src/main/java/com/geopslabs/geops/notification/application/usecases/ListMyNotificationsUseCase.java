package com.geopslabs.geops.notification.application.usecases;

import java.util.List;

public interface ListMyNotificationsUseCase { List<InboxNotificationView> list(Long recipientId, int size); }

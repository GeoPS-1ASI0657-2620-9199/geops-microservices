package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.PushSubscription;
public interface RegisterPushSubscriptionUseCase {
    Result register(Long consumerId, String token, String userAgent);
    record Result(PushSubscription subscription, boolean created) {}
}

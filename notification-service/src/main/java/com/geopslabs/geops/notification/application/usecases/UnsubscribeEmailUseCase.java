package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.EmailUnsubscription;
public interface UnsubscribeEmailUseCase { EmailUnsubscription unsubscribe(String token); }

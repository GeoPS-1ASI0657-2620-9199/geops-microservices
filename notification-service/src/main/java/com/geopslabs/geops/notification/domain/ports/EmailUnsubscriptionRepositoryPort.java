package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.*;
public interface EmailUnsubscriptionRepositoryPort {
    EmailUnsubscription saveIfAbsent(EmailUnsubscription unsubscription);
    boolean exists(Long consumerId, NotificationType type);
}

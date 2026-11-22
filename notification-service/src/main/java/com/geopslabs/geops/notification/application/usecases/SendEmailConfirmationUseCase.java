package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.ConfirmationLink;

public interface SendEmailConfirmationUseCase {
    void send(ConfirmationLink link);
}

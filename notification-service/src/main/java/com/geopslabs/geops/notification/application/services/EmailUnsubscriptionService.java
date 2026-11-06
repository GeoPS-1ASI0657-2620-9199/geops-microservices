package com.geopslabs.geops.notification.application.services;

import com.geopslabs.geops.notification.application.usecases.UnsubscribeEmailUseCase;
import com.geopslabs.geops.notification.domain.models.*;
import com.geopslabs.geops.notification.domain.ports.EmailUnsubscriptionRepositoryPort;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

public class EmailUnsubscriptionService implements UnsubscribeEmailUseCase {
    private final EmailUnsubscriptionRepositoryPort unsubscriptions;
    private final String secret;
    private final Clock clock;
    public EmailUnsubscriptionService(EmailUnsubscriptionRepositoryPort unsubscriptions, String secret, Clock clock) { this.unsubscriptions = unsubscriptions; this.secret = secret; this.clock = clock; }
    @Override
    @Transactional
    public EmailUnsubscription unsubscribe(String token) {
        var verified = UnsubscribeToken.verify(token, secret);
        return unsubscriptions.saveIfAbsent(new EmailUnsubscription(verified.consumerId(), verified.type(), token, clock.instant()));
    }
}

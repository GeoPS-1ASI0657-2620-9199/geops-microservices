package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.ConfirmEmailUseCase;
import com.geopslabs.geops.identity.domain.events.EmailConfirmed;
import com.geopslabs.geops.identity.domain.models.EmailConfirmation;
import com.geopslabs.geops.identity.domain.models.InvalidEmailConfirmationException;
import com.geopslabs.geops.identity.domain.ports.EmailConfirmationRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.OutboxPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Confirms the email with the token of the link (US28). The confirmation, the removal of the used link and the
 * EmailConfirmed event are one transaction; Notification learns about it from the event, never from a call.
 */
@Transactional
public class EmailConfirmationService implements ConfirmEmailUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailConfirmationService.class);

    private final EmailConfirmationRepositoryPort emailConfirmations;
    private final UserRepositoryPort userRepository;
    private final OutboxPort outbox;
    private final Clock clock;
    private final Supplier<UUID> eventIds;

    public EmailConfirmationService(EmailConfirmationRepositoryPort emailConfirmations,
                                    UserRepositoryPort userRepository, OutboxPort outbox, Clock clock,
                                    Supplier<UUID> eventIds) {
        this.emailConfirmations = emailConfirmations;
        this.userRepository = userRepository;
        this.outbox = outbox;
        this.clock = clock;
        this.eventIds = eventIds;
    }

    @Override
    public void confirm(String token) {
        var now = clock.instant();
        var confirmation = emailConfirmations.findByTokenHash(EmailConfirmation.hashOf(token))
                .orElseThrow(() -> rejected("unknown-token"));
        if (confirmation.isExpiredAt(now)) {
            throw rejected("expired-token");
        }
        var user = userRepository.findById(confirmation.userId()).orElseThrow(() -> rejected("unknown-user"));
        emailConfirmations.deleteByUserId(user.getId());
        if (user.confirmEmail(now)) {
            var confirmed = userRepository.save(user);
            outbox.append(EmailConfirmed.of(confirmed, eventIds.get()));
        }
        LOGGER.info("user.email-confirmed userId={}", user.getId());
    }

    private static InvalidEmailConfirmationException rejected(String reason) {
        LOGGER.warn("user.email-confirmation.rejected reason={}", reason);
        return new InvalidEmailConfirmationException();
    }
}

package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.LogInCommand;
import com.geopslabs.geops.identity.application.usecases.LogInResult;
import com.geopslabs.geops.identity.application.usecases.LogInUseCase;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.InvalidCredentialsException;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.TokenIssuerPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Transactional(noRollbackFor = InvalidCredentialsException.class)
public class UserAuthenticationService implements LogInUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAuthenticationService.class);

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;
    private final Clock clock;
    private final String fillerHash;

    public UserAuthenticationService(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher,
                                     TokenIssuerPort tokenIssuer, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
        this.fillerHash = passwordHasher.encode(UUID.randomUUID().toString());
    }

    @Override
    public LogInResult logIn(LogInCommand command) {
        var user = userRepository.findByEmail(new Email(command.email()).value())
                .orElseThrow(() -> rejectUnknownEmail(command.password()));
        var now = clock.instant();
        ensureNotLocked(user, command.password(), now);
        ensurePasswordMatches(user, command.password(), now);
        user.resetFailedLogins();
        userRepository.save(user);
        var consumerId = consumerIdOf(user);
        LOGGER.info("user.logged-in userId={} role={}", user.getId(), user.getRole());
        return new LogInResult(tokenIssuer.issue(user, consumerId), user.getId(), user.getRole(), consumerId);
    }

    private InvalidCredentialsException rejectUnknownEmail(String password) {
        passwordHasher.matches(password, fillerHash);
        LOGGER.warn("user.login.rejected reason=unknown-email");
        return new InvalidCredentialsException();
    }

    private void ensureNotLocked(User user, String password, Instant now) {
        if (user.isLockedAt(now)) {
            passwordHasher.matches(password, user.getPasswordHash());
            LOGGER.warn("user.login.rejected reason=locked userId={}", user.getId());
            throw new InvalidCredentialsException();
        }
    }

    private void ensurePasswordMatches(User user, String password, Instant now) {
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            user.registerFailedLogin(now);
            userRepository.save(user);
            LOGGER.warn("user.login.rejected reason=wrong-password userId={}", user.getId());
            throw new InvalidCredentialsException();
        }
    }

    private static Long consumerIdOf(User user) {
        return user.getRole() == Role.CONSUMER ? user.getId() : null;
    }
}

package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.RegisterUserCommand;
import com.geopslabs.geops.identity.application.usecases.RegisterUserUseCase;
import com.geopslabs.geops.identity.application.usecases.RegisteredUser;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.EmailAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.PhoneAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.RoleNotAllowedException;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class UserCommandService implements RegisterUserUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserCommandService.class);

    private final UserRepositoryPort userRepository;
    private final ConsumerProfileRepositoryPort consumerProfileRepository;
    private final PasswordHasherPort passwordHasher;

    public UserCommandService(UserRepositoryPort userRepository,
                              ConsumerProfileRepositoryPort consumerProfileRepository,
                              PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.consumerProfileRepository = consumerProfileRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public RegisteredUser register(RegisterUserCommand command) {
        var email = new Email(command.email());
        ensureRoleIsAllowed(command.role());
        ensureEmailIsAvailable(email);
        ensurePhoneIsAvailable(command.phone());
        var user = userRepository.save(User.register(command.fullName(), email, command.phone(),
                passwordHasher.encode(command.password()), command.role()));
        var profile = consumerProfileRepository.save(ConsumerProfile.createFor(user.getId()));
        LOGGER.info("user.registered userId={} role={}", user.getId(), user.getRole());
        return new RegisteredUser(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), profile.getId());
    }

    private void ensureRoleIsAllowed(Role role) {
        if (!role.isSelfRegistrable()) {
            LOGGER.warn("user.registration.rejected reason=role-not-allowed role={}", role);
            throw new RoleNotAllowedException();
        }
    }

    private void ensureEmailIsAvailable(Email email) {
        if (userRepository.existsByEmail(email.value())) {
            LOGGER.warn("user.registration.rejected reason=email-already-registered");
            throw new EmailAlreadyRegisteredException();
        }
    }

    private void ensurePhoneIsAvailable(String phone) {
        if (userRepository.existsByPhone(phone)) {
            LOGGER.warn("user.registration.rejected reason=phone-already-registered");
            throw new PhoneAlreadyRegisteredException();
        }
    }
}

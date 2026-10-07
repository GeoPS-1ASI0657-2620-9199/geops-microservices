package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.BusinessProfileData;
import com.geopslabs.geops.identity.application.usecases.RegisterUserCommand;
import com.geopslabs.geops.identity.application.usecases.RegisterUserUseCase;
import com.geopslabs.geops.identity.application.usecases.RegisteredUser;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.BusinessProfileNotAllowedException;
import com.geopslabs.geops.identity.domain.models.BusinessProfileRequiredException;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.models.Email;
import com.geopslabs.geops.identity.domain.models.EmailAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.GeoPoint;
import com.geopslabs.geops.identity.domain.models.PhoneAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.RoleNotAllowedException;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.RucAlreadyRegisteredException;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
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
    private final BusinessProfileRepositoryPort businessProfileRepository;
    private final PasswordHasherPort passwordHasher;

    public UserCommandService(UserRepositoryPort userRepository,
                              ConsumerProfileRepositoryPort consumerProfileRepository,
                              BusinessProfileRepositoryPort businessProfileRepository,
                              PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.consumerProfileRepository = consumerProfileRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public RegisteredUser register(RegisterUserCommand command) {
        ensureRoleIsAllowed(command.role());
        ensureBusinessProfileMatchesRole(command);
        return command.role().requiresBusinessProfile()
                ? registerBusinessOwner(command)
                : registerConsumer(command);
    }

    private RegisteredUser registerConsumer(RegisterUserCommand command) {
        var email = new Email(command.email());
        ensureAccountIsAvailable(email, command.phone());
        var user = saveUser(command, email);
        var profile = consumerProfileRepository.save(ConsumerProfile.createFor(user.getId()));
        LOGGER.info("user.registered userId={} role={}", user.getId(), user.getRole());
        return RegisteredUser.consumer(user, profile);
    }

    private RegisteredUser registerBusinessOwner(RegisterUserCommand command) {
        var email = new Email(command.email());
        var draft = businessProfileFrom(command.businessProfile());
        ensureAccountIsAvailable(email, command.phone());
        ensureRucIsAvailable(draft.getRuc());
        var user = saveUser(command, email);
        var profile = businessProfileRepository.save(draft.ownedBy(user.getId()));
        LOGGER.info("user.registered userId={} role={} businessProfileId={}", user.getId(), user.getRole(),
                profile.getId());
        return RegisteredUser.businessOwner(user, profile);
    }

    private User saveUser(RegisterUserCommand command, Email email) {
        return userRepository.save(User.register(command.fullName(), email, command.phone(),
                passwordHasher.encode(command.password()), command.role()));
    }

    private static BusinessProfile businessProfileFrom(BusinessProfileData data) {
        return BusinessProfile.register(data.businessName(), data.businessType(), new Ruc(data.ruc()),
                data.address(), GeoPoint.of(data.latitude(), data.longitude()), data.openingHours());
    }

    private void ensureRoleIsAllowed(Role role) {
        if (!role.isSelfRegistrable()) {
            LOGGER.warn("user.registration.rejected reason=role-not-allowed role={}", role);
            throw new RoleNotAllowedException();
        }
    }

    private void ensureBusinessProfileMatchesRole(RegisterUserCommand command) {
        var hasBusinessProfile = command.businessProfile() != null;
        if (command.role().requiresBusinessProfile() && !hasBusinessProfile) {
            LOGGER.warn("user.registration.rejected reason=business-profile-required");
            throw new BusinessProfileRequiredException();
        }
        if (!command.role().requiresBusinessProfile() && hasBusinessProfile) {
            LOGGER.warn("user.registration.rejected reason=business-profile-not-allowed role={}", command.role());
            throw new BusinessProfileNotAllowedException();
        }
    }

    private void ensureAccountIsAvailable(Email email, String phone) {
        if (userRepository.existsByEmail(email.value())) {
            LOGGER.warn("user.registration.rejected reason=email-already-registered");
            throw new EmailAlreadyRegisteredException();
        }
        if (userRepository.existsByPhone(phone)) {
            LOGGER.warn("user.registration.rejected reason=phone-already-registered");
            throw new PhoneAlreadyRegisteredException();
        }
    }

    private void ensureRucIsAvailable(Ruc ruc) {
        if (businessProfileRepository.existsByRuc(ruc)) {
            LOGGER.warn("user.registration.rejected reason=ruc-already-registered");
            throw new RucAlreadyRegisteredException();
        }
    }
}

package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.CreateUserCommand;
import com.geopslabs.geops.identity.application.usecases.DeleteUserCommand;
import com.geopslabs.geops.identity.application.usecases.UpdateUserCommand;
import com.geopslabs.geops.identity.application.usecases.UserCommandUseCase;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.PasswordHasherPort;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public class UserCommandService implements UserCommandUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserCommandService.class);

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    public UserCommandService(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Optional<User> handle(CreateUserCommand command) {
        if (isEmailOrPhoneTaken(command.email(), command.phone())) {
            return Optional.empty();
        }
        var user = new User(command.name(), command.email(), command.phone(),
                passwordHasher.encode(command.password()), command.role());
        return Optional.of(userRepository.save(user));
    }

    @Override
    public Optional<User> handle(UpdateUserCommand command) {
        var userOptional = userRepository.findById(command.id());
        if (userOptional.isEmpty() || isUpdateConflicting(command, userOptional.get())) {
            return Optional.empty();
        }
        var user = userOptional.get();
        user.updateUser(command.name(), command.email(), command.phone(), command.role());
        return Optional.of(userRepository.save(user));
    }

    @Override
    public boolean handle(DeleteUserCommand command) {
        if (!userRepository.existsById(command.id())) {
            LOGGER.warn("User {} not found", command.id());
            return false;
        }
        userRepository.deleteById(command.id());
        return true;
    }

    private boolean isEmailOrPhoneTaken(String email, String phone) {
        var taken = userRepository.existsByEmail(email) || userRepository.existsByPhone(phone);
        if (taken) {
            LOGGER.warn("Email or phone already registered");
        }
        return taken;
    }

    private boolean isUpdateConflicting(UpdateUserCommand command, User user) {
        var emailTaken = command.email() != null && !command.email().equals(user.getEmail())
                && userRepository.existsByEmail(command.email());
        var phoneTaken = command.phone() != null && !command.phone().equals(user.getPhone())
                && userRepository.existsByPhone(command.phone());
        return emailTaken || phoneTaken;
    }
}

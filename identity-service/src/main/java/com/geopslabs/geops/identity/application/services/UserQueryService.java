package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.GetAllUsersQuery;
import com.geopslabs.geops.identity.application.usecases.GetUserByEmailQuery;
import com.geopslabs.geops.identity.application.usecases.GetUserByIdQuery;
import com.geopslabs.geops.identity.application.usecases.GetUserByPhoneQuery;
import com.geopslabs.geops.identity.application.usecases.UserQueryUseCase;
import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public class UserQueryService implements UserQueryUseCase {
    private final UserRepositoryPort userRepository;

    public UserQueryService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> handle(GetAllUsersQuery query) {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.id());
    }

    @Override
    public Optional<User> handle(GetUserByEmailQuery query) {
        return userRepository.findByEmail(query.email());
    }

    @Override
    public Optional<User> handle(GetUserByPhoneQuery query) {
        return userRepository.findByPhone(query.phone());
    }
}

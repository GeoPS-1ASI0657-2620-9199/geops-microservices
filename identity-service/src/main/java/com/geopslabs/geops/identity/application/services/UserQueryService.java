package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.application.usecases.GetAllUsersQuery;
import com.geopslabs.geops.identity.application.usecases.GetUserByEmailQuery;
import com.geopslabs.geops.identity.application.usecases.GetUserByIdQuery;
import com.geopslabs.geops.identity.application.usecases.GetUserByPhoneQuery;
import com.geopslabs.geops.identity.application.usecases.UserQueryUseCase;
import com.geopslabs.geops.identity.infrastructure.persistence.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * UserQueryService
 *
 * Implementation of the UserQueryUseCase that handles all query operations
 * for users. This service implements the business logic for retrieving and
 * searching users following DDD principles
 *
 * @summary Implementation of user query service operations
 * @since 1.0
 * @author GeOps Labs
 */
@Service
@Transactional(readOnly = true)
public class UserQueryService implements UserQueryUseCase {

    private final UserJpaRepository userRepository;

    /**
     * Constructor for dependency injection
     *
     * @param userRepository The repository for user data access
     */
    public UserQueryService(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> handle(GetAllUsersQuery query) {
        try {
            return userRepository.findAll();
        } catch (Exception e) {
            System.err.println("Error retrieving all users: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        try {
            return userRepository.findById(query.id());
        } catch (Exception e) {
            System.err.println("Error retrieving user by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> handle(GetUserByEmailQuery query) {
        try {
            return userRepository.findByEmail(query.email());
        } catch (Exception e) {
            System.err.println("Error retrieving user by email: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> handle(GetUserByPhoneQuery query) {
        try {
            return userRepository.findByPhone(query.phone());
        } catch (Exception e) {
            System.err.println("Error retrieving user by phone: " + e.getMessage());
            return Optional.empty();
        }
    }
}


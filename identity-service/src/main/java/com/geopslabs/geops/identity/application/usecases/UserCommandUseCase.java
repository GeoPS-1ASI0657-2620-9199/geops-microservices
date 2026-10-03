package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.User;

import java.util.Optional;

/**
 * UserCommandUseCase
 *
 * Service interface for handling user command operations
 * This service defines methods for creating, updating, and deleting users
 * following the DDD pattern
 *
 * @summary Service for user command operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface UserCommandUseCase {

    /**
     * Handles the command to create a new user
     *
     * @param command The CreateUserCommand containing user data
     * @return An Optional containing the created user if successful, empty otherwise
     */
    Optional<User> handle(CreateUserCommand command);

    /**
     * Handles the command to update an existing user
     *
     * @param command The UpdateUserCommand containing updated user data
     * @return An Optional containing the updated user if successful, empty otherwise
     */
    Optional<User> handle(UpdateUserCommand command);

    /**
     * Handles the command to delete a user
     *
     * @param command The DeleteUserCommand containing the user ID to delete
     * @return true if the user was deleted successfully, false otherwise
     */
    boolean handle(DeleteUserCommand command);
}


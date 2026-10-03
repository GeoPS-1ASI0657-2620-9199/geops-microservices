package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.User;

import java.util.List;
import java.util.Optional;

/**
 * UserQueryUseCase
 *
 * Service interface for handling user query operations
 * This service defines methods for retrieving user information
 * following the DDD pattern
 *
 * @summary Service for user query operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface UserQueryUseCase {

    /**
     * Handles the query to get all users
     *
     * @param query The GetAllUsersQuery
     * @return A list of all users in the system
     */
    List<User> handle(GetAllUsersQuery query);

    /**
     * Handles the query to get a user by ID
     *
     * @param query The GetUserByIdQuery containing the user ID
     * @return An Optional containing the user if found, empty otherwise
     */
    Optional<User> handle(GetUserByIdQuery query);

    /**
     * Handles the query to get a user by email
     *
     * @param query The GetUserByEmailQuery containing the user email
     * @return An Optional containing the user if found, empty otherwise
     */
    Optional<User> handle(GetUserByEmailQuery query);

    /**
     * Handles the query to get a user by phone
     *
     * @param query The GetUserByPhoneQuery containing the user phone
     * @return An Optional containing the user if found, empty otherwise
     */
    Optional<User> handle(GetUserByPhoneQuery query);
}


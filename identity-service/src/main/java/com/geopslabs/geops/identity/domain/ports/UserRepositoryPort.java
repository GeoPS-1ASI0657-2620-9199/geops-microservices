package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.User;

import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}

package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);

    List<User> findAll();

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsById(Long id);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    void deleteById(Long id);
}

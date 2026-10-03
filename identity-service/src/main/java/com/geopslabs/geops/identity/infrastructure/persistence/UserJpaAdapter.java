package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.User;
import com.geopslabs.geops.identity.domain.ports.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserJpaAdapter implements UserRepositoryPort {
    private final UserJpaRepository repository;

    public UserJpaAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        return UserPersistenceMapper.toDomain(repository.save(UserPersistenceMapper.toEntity(user)));
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream().map(UserPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return repository.findByPhone(phone).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return repository.existsByPhone(phone);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}

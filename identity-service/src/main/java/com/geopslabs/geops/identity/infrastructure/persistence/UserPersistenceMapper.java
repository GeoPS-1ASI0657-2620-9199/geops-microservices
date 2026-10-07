package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.Role;
import com.geopslabs.geops.identity.domain.models.User;

public final class UserPersistenceMapper {
    private UserPersistenceMapper() {
    }

    public static User toDomain(UserJpaEntity entity) {
        var data = new User(entity.getFullName(), entity.getEmail(), entity.getPhone(), entity.getPasswordHash(),
                Role.valueOf(entity.getRole()));
        return new User(entity.getId(), data, entity.getEmailConfirmedAt(), entity.getFailedLoginAttempts(),
                entity.getLockedUntil(), entity.getCreatedAt());
    }

    public static UserJpaEntity toEntity(User user) {
        var entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setFullName(user.getFullName());
        entity.setEmail(user.getEmail());
        entity.setEmailConfirmedAt(user.getEmailConfirmedAt());
        entity.setPhone(user.getPhone());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRole(user.getRole().name());
        entity.setFailedLoginAttempts(user.getFailedLoginAttempts());
        entity.setLockedUntil(user.getLockedUntil());
        entity.setCreatedAt(user.getCreatedAt());
        return entity;
    }
}

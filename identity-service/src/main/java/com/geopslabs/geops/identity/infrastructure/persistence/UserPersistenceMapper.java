package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.User;

public final class UserPersistenceMapper {
    private UserPersistenceMapper() {
    }

    public static User toDomain(UserJpaEntity entity) {
        var data = new User(entity.getName(), entity.getEmail(), entity.getPhone(), entity.getPassword(),
                entity.getRole(), entity.getPlan());
        return new User(entity.getId(), data, entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static UserJpaEntity toNewEntity(User user) {
        return new UserJpaEntity(user.getName(), user.getEmail(), user.getPhone(), user.getPassword(),
                user.getRole(), user.getPlan());
    }

    public static void copyToEntity(User user, UserJpaEntity entity) {
        entity.update(user.getName(), user.getEmail(), user.getPhone(), user.getPassword(),
                user.getRole(), user.getPlan());
    }
}

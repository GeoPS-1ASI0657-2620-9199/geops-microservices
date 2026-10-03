package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.shared.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "users")
@Getter
public class UserJpaEntity extends AuditableAbstractAggregateRoot<UserJpaEntity> {
    private static final int NAME_LENGTH = 255;
    private static final int EMAIL_LENGTH = 255;
    private static final int PHONE_LENGTH = 20;
    private static final int PASSWORD_LENGTH = 255;
    private static final int ROLE_LENGTH = 50;

    @Column(name = "name", nullable = false, length = NAME_LENGTH)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = EMAIL_LENGTH)
    private String email;

    @Column(name = "phone", nullable = false, unique = true, length = PHONE_LENGTH)
    private String phone;

    @Column(name = "password", nullable = false, length = PASSWORD_LENGTH)
    private String password;

    @Column(name = "role", nullable = false, length = ROLE_LENGTH)
    private String role;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(String name, String email, String phone, String password, String role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }

    public void update(String name, String email, String phone, String password, String role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }
}

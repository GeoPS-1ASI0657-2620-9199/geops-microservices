package com.geopslabs.geops.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recipients")
@Getter
@Setter
public class RecipientJpaEntity {
    private static final int EMAIL_LENGTH = 255;
    private static final int ROLE_LENGTH = 20;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, length = EMAIL_LENGTH)
    private String email;

    @Column(name = "email_confirmed", nullable = false)
    private Boolean emailConfirmed;

    @Column(name = "role", nullable = false, length = ROLE_LENGTH)
    private String role;
}

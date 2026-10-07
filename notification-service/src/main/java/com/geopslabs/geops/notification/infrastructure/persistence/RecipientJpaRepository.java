package com.geopslabs.geops.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RecipientJpaRepository extends JpaRepository<RecipientJpaEntity, Long> {

    boolean existsByUserIdAndRole(Long userId, String role);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO recipients (user_id, email, email_confirmed, role)
            VALUES (:userId, :email, :emailConfirmed, :role)
            ON CONFLICT (user_id) DO UPDATE SET email = EXCLUDED.email,
                email_confirmed = EXCLUDED.email_confirmed, role = EXCLUDED.role""",
            nativeQuery = true)
    int upsert(@Param("userId") Long userId, @Param("email") String email,
               @Param("emailConfirmed") Boolean emailConfirmed, @Param("role") String role);
}

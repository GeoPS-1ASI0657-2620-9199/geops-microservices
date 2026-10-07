package com.geopslabs.geops.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface NotificationPreferenceJpaRepository extends JpaRepository<NotificationPreferenceJpaEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = """
            INSERT INTO notification_preferences (consumer_id, push_enabled, email_enabled, daily_limit, updated_at)
            VALUES (:consumerId, :pushEnabled, :emailEnabled, :dailyLimit, :updatedAt)
            ON CONFLICT (consumer_id) DO UPDATE SET push_enabled = EXCLUDED.push_enabled,
                email_enabled = EXCLUDED.email_enabled, daily_limit = EXCLUDED.daily_limit,
                updated_at = EXCLUDED.updated_at""",
            nativeQuery = true)
    int upsert(@Param("consumerId") Long consumerId, @Param("pushEnabled") Boolean pushEnabled,
               @Param("emailEnabled") Boolean emailEnabled, @Param("dailyLimit") Integer dailyLimit,
               @Param("updatedAt") Instant updatedAt);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = """
            INSERT INTO notification_preferences (consumer_id, push_enabled, email_enabled, daily_limit, updated_at)
            VALUES (:consumerId, :pushEnabled, :emailEnabled, :dailyLimit, :updatedAt)
            ON CONFLICT (consumer_id) DO NOTHING""",
            nativeQuery = true)
    int insertIfAbsent(@Param("consumerId") Long consumerId, @Param("pushEnabled") Boolean pushEnabled,
                       @Param("emailEnabled") Boolean emailEnabled, @Param("dailyLimit") Integer dailyLimit,
                       @Param("updatedAt") Instant updatedAt);
}

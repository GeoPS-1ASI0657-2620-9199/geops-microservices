package com.geopslabs.geops.identity;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/** V5 writes one UserRegistered per user that already exists, except administrators. */
class UserRegisteredBackfillTest {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static {
        POSTGRES.start();
    }

    @Test
    void everyExistingUserButTheAdministratorGetsItsEventWithTheBusinessProfileId() throws SQLException {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("4").load().migrate();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()); var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO users (id, full_name, email, phone, password_hash, role, failed_login_attempts, created_at) VALUES
                    (1, 'Lucía', 'lucia@ejemplo.pe', '987000001', 'h', 'CONSUMER', 0, '2026-10-01T10:00:00Z'),
                    (2, 'Rosa', 'rosa@ejemplo.pe', '987000002', 'h', 'BUSINESS_OWNER', 0, '2026-10-02T10:00:00Z'),
                    (3, 'Admin', 'admin@ejemplo.pe', '987000003', 'h', 'ADMIN', 0, '2026-10-03T10:00:00Z')""");
            statement.execute("""
                    INSERT INTO business_profiles (id, user_id, business_name, ruc, address, account_status, verification_status)
                    VALUES (84, 2, 'Bodega', '10456789019', 'Jr. Huánuco 1250', 'ACTIVE', 'PENDING')""");
        }
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();

        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()); var statement = connection.createStatement();
             var rows = statement.executeQuery("""
                     SELECT aggregate_id, payload->>'eventId' = id::text, payload->'payload'->>'role',
                            payload->'payload'->>'businessId', payload->>'occurredAt', status
                     FROM outbox_events WHERE event_type = 'UserRegistered' ORDER BY aggregate_id""")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong(1)).isEqualTo(1L);
            assertThat(rows.getBoolean(2)).isTrue();
            assertThat(rows.getString(3)).isEqualTo("CONSUMER");
            assertThat(rows.getString(4)).isNull();
            assertThat(rows.getString(5)).isEqualTo("2026-10-01T10:00:00Z");
            assertThat(rows.getString(6)).isEqualTo("PENDING");
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong(1)).isEqualTo(2L);
            assertThat(rows.getString(4)).isEqualTo("84");
            assertThat(rows.next()).as("the administrator has no event").isFalse();
        }
    }
}

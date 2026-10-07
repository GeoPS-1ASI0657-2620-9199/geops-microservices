package com.geopslabs.geops.engagement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final String MIGRATIONS_LOCATION = "classpath:db/migration";
    private static final String SCHEMA_QUERY = """
            SELECT table_name || '.' || column_name || ':' || data_type
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
            ORDER BY table_name, ordinal_position""";
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);

    static {
        POSTGRES.start();
    }

    @Test
    void migrationsRerunWithoutChangesOnAnEmptyDatabase() throws SQLException {
        var flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(MIGRATIONS_LOCATION)
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isPositive();
        var schemaAfterFirstRun = currentSchema();

        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(currentSchema()).isEqualTo(schemaAfterFirstRun);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    private static List<String> currentSchema() throws SQLException {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword());
             var statement = connection.createStatement();
             var rows = statement.executeQuery(SCHEMA_QUERY)) {
            var schema = new ArrayList<String>();
            while (rows.next()) {
                schema.add(rows.getString(1));
            }
            return schema;
        }
    }
}

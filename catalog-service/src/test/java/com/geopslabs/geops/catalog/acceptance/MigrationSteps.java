package com.geopslabs.geops.catalog.acceptance;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationSteps {
    private static final String MIGRATIONS_LOCATION = "classpath:db/migration";
    private static final String MIGRATION_FILES = MIGRATIONS_LOCATION + "/*.sql";
    private static final String VERSIONED_MIGRATION_NAME = "V\\d+__[a-z0-9_]+\\.sql";
    private static final String DDL_AUTO_PROPERTY = "spring.jpa.hibernate.ddl-auto";
    private static final String VALIDATE = "validate";
    private static final String DATABASE_PREFIX = "migrations_";
    private static final String DATABASE_IN_URL = "/[^/?]+(\\?|$)";
    private static final String CREATE_DATABASE = "CREATE DATABASE %s";
    private static final String DROP_DATABASE = "DROP DATABASE IF EXISTS %s WITH (FORCE)";
    private static final String ENABLE_POSTGIS = "CREATE EXTENSION IF NOT EXISTS postgis";
    private static final String COLUMNS = """
            SELECT table_name || '.' || column_name || ':' || data_type || ':' || is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name NOT IN ('flyway_schema_history', 'spatial_ref_sys')
            ORDER BY table_name, column_name""";
    private static final String INDEXES = """
            SELECT indexdef FROM pg_indexes
            WHERE schemaname = 'public' AND tablename NOT IN ('flyway_schema_history', 'spatial_ref_sys')
            ORDER BY indexdef""";
    private static final String CONSTRAINTS = """
            SELECT conrelid::regclass || ':' || conname || ':' || pg_get_constraintdef(oid)
            FROM pg_constraint
            WHERE connamespace = 'public'::regnamespace
              AND conrelid::regclass::text NOT IN ('flyway_schema_history', 'spatial_ref_sys')
            ORDER BY 1""";
    private static final String TABLE_COLUMNS = """
            SELECT table_name || '.' || column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'""";
    private static final String COLUMN_SEPARATOR = ".";
    private static final int NO_MIGRATIONS = 0;
    private static final int SHORT_NAME_LENGTH = 8;

    private final EntityManagerFactory entityManagerFactory;
    private final Environment environment;
    private String databaseName;
    private DataSource migrationDataSource;
    private MigrateResult lastResult;
    private List<String> schemaBeforeRerun;

    public MigrationSteps(EntityManagerFactory entityManagerFactory, Environment environment) {
        this.entityManagerFactory = entityManagerFactory;
        this.environment = environment;
    }

    @After("@GEO-51")
    public void dropTheMigrationDatabase() {
        if (databaseName != null) {
            adminJdbc().execute(DROP_DATABASE.formatted(databaseName));
        }
    }

    @Given("an empty database with PostGIS")
    public void anEmptyDatabaseWithPostgis() {
        databaseName = DATABASE_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, SHORT_NAME_LENGTH);
        adminJdbc().execute(CREATE_DATABASE.formatted(databaseName));
        migrationDataSource = dataSourceFor(databaseName);
        new JdbcTemplate(migrationDataSource).execute(ENABLE_POSTGIS);
    }

    @When("the migrations run in order")
    public void theMigrationsRunInOrder() {
        lastResult = flyway().migrate();
    }

    @And("the migrations already ran")
    public void theMigrationsAlreadyRan() {
        lastResult = flyway().migrate();
    }

    @When("the migrations run again")
    public void theMigrationsRunAgain() {
        schemaBeforeRerun = schemaOf(migrationDataSource);
        lastResult = flyway().migrate();
    }

    @Then("every pending migration is applied successfully")
    public void everyPendingMigrationIsApplied() {
        assertThat(lastResult.success).isTrue();
        assertThat(lastResult.migrationsExecuted).isEqualTo(migrationFiles().size());
    }

    @And("every table and column the service maps exists in the resulting schema")
    public void everyMappedColumnExists() {
        var mapped = mappedColumns();
        var migrated = new JdbcTemplate(migrationDataSource).queryForList(TABLE_COLUMNS, String.class);
        assertThat(mapped).isNotEmpty();
        assertThat(migrated).containsAll(mapped);
    }

    @Then("no migration is applied")
    public void noMigrationIsApplied() {
        assertThat(lastResult.success).isTrue();
        assertThat(lastResult.migrationsExecuted).isEqualTo(NO_MIGRATIONS);
    }

    @And("the schema is the same as before the second run")
    public void theSchemaIsUnchanged() {
        assertThat(schemaOf(migrationDataSource)).isEqualTo(schemaBeforeRerun);
    }

    @Then("Hibernate only validates the schema")
    public void hibernateOnlyValidatesTheSchema() {
        assertThat(environment.getProperty(DDL_AUTO_PROPERTY)).isEqualTo(VALIDATE);
    }

    @And("every migration file is a versioned migration")
    public void everyMigrationFileIsVersioned() {
        assertThat(migrationFiles()).isNotEmpty().allMatch(name -> name.matches(VERSIONED_MIGRATION_NAME));
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(migrationDataSource)
                .locations(MIGRATIONS_LOCATION)
                .load();
    }

    private List<String> mappedColumns() {
        var columns = new ArrayList<String>();
        entityManagerFactory.unwrap(SessionFactoryImplementor.class).getMappingMetamodel()
                .forEachEntityDescriptor(descriptor -> columns.addAll(columnsOf((AbstractEntityPersister) descriptor)));
        return columns;
    }

    private static List<String> columnsOf(AbstractEntityPersister persister) {
        var table = persister.getTableName();
        var propertyColumns = IntStream.range(0, persister.getPropertySpan())
                .mapToObj(persister::getPropertyColumnNames)
                .flatMap(Arrays::stream);
        return Stream.concat(Arrays.stream(persister.getIdentifierColumnNames()), propertyColumns)
                .map(column -> table + COLUMN_SEPARATOR + column)
                .toList();
    }

    private static List<String> schemaOf(DataSource dataSource) {
        var jdbc = new JdbcTemplate(dataSource);
        return List.of(COLUMNS, INDEXES, CONSTRAINTS).stream()
                .flatMap(query -> jdbc.queryForList(query, String.class).stream())
                .toList();
    }

    private static List<String> migrationFiles() {
        try {
            var resources = new PathMatchingResourcePatternResolver().getResources(MIGRATION_FILES);
            return Arrays.stream(resources).map(resource -> resource.getFilename()).toList();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static JdbcTemplate adminJdbc() {
        var container = CucumberSpringConfiguration.POSTGRES;
        return new JdbcTemplate(new DriverManagerDataSource(container.getJdbcUrl(), container.getUsername(),
                container.getPassword()));
    }

    private static DataSource dataSourceFor(String database) {
        var container = CucumberSpringConfiguration.POSTGRES;
        var url = container.getJdbcUrl().replaceFirst(DATABASE_IN_URL, "/" + database + "$1");
        return new DriverManagerDataSource(url, container.getUsername(), container.getPassword());
    }
}

package com.geopslabs.geops.identity.acceptance;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/** US25 and US28: the schema of Identity and its import graph, checked from the running service. */
public class IdentitySchemaSteps {
    private static final String ROOT_PACKAGE = "com.geopslabs.geops";
    private static final String OWN_PACKAGE = ROOT_PACKAGE + ".identity";
    private static final String[] OTHER_CONTEXTS = {
            ROOT_PACKAGE + ".catalog..", ROOT_PACKAGE + ".reservation..", ROOT_PACKAGE + ".engagement..",
            ROOT_PACKAGE + ".notification.."
    };
    private static final String[] PROVIDER_ADAPTERS = {
            OWN_PACKAGE + ".infrastructure.geocoding..", OWN_PACKAGE + ".infrastructure.sunat.."
    };
    private static final String[] HTTP_CLIENTS = {
            "org.springframework.web.client..", "org.springframework.web.reactive.function.client..",
            "java.net.http.."
    };
    /** The tables that the migrations of Catalog, Reservation, Engagement and Notification create. */
    private static final Set<String> TABLES_OF_OTHER_CONTEXTS = Set.of(
            "business_baselines", "campaign_daily_metrics", "campaigns", "merchant_standings", "moderation_actions",
            "offers", "standing_changes", "noncompliance_reports", "report_responses", "reservations",
            "business_snapshots", "follows", "offer_snapshots", "redeemed_reservations", "review_replies", "reviews",
            "saved_businesses", "saved_offers", "confirmation_emails", "email_digests", "email_unsubscriptions",
            "followed_businesses", "last_known_locations", "notification_preferences", "notifications",
            "push_subscriptions", "recipients");
    private static final String FOREIGN_KEY_TARGETS = """
            SELECT target.relname FROM pg_constraint c
            JOIN pg_class target ON target.oid = c.confrelid
            JOIN pg_namespace n ON n.oid = target.relnamespace
            WHERE c.contype = 'f' AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'topology', 'tiger')""";
    private static final long POOL_VALIDATION_WAIT_MS = 1_000;
    private static final String OWN_TABLES ="SELECT tablename FROM pg_tables WHERE schemaname = 'public'";

    private final JdbcTemplate jdbc;
    private List<String> foreignKeyTargets;
    private JavaClasses classes;

    public IdentitySchemaSteps(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @When("the foreign keys of the identity schema are listed")
    public void theForeignKeysAreListed() {
        foreignKeyTargets = jdbc.queryForList(FOREIGN_KEY_TARGETS, String.class);
    }

    @Then("every foreign key points to a table of the identity schema")
    public void everyForeignKeyIsInternal() {
        assertThat(foreignKeyTargets).isNotEmpty();
        assertThat(jdbc.queryForList(OWN_TABLES, String.class)).containsAll(foreignKeyTargets);
    }

    @And("the identity schema has no table of another context")
    public void noTableOfAnotherContext() {
        assertThat(jdbc.queryForList(OWN_TABLES, String.class)).doesNotContainAnyElementsOf(TABLES_OF_OTHER_CONTEXTS);
    }

    @When("the database closes every connection of Identity")
    public void theDatabaseClosesTheConnections() {
        // Same effect on the pool as a restart of PostgreSQL: every open connection of the service is cut.
        jdbc.query("SELECT pg_terminate_backend(pid) FROM pg_stat_activity "
                + "WHERE datname = current_database() AND pid <> pg_backend_pid()", resultSet -> { });
        // The pool trusts a connection used in the last 500 ms without testing it; the request comes later.
        try {
            Thread.sleep(POOL_VALIDATION_WAIT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @When("the import graph of Identity is analyzed")
    public void theImportGraphIsAnalyzed() {
        classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(OWN_PACKAGE);
    }

    @Then("no class of Identity depends on Notification")
    public void noDependencyOnNotification() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAPackage(ROOT_PACKAGE + ".notification..")
                .check(classes);
    }

    @Then("no class of Identity depends on another context")
    public void noDependencyOnAnotherContext() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAnyPackage(OTHER_CONTEXTS)
                .check(classes);
    }

    @And("only the adapters of the external providers make HTTP calls")
    public void onlyProviderAdaptersCallHttp() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..").and().resideOutsideOfPackages(PROVIDER_ADAPTERS)
                .should().dependOnClassesThat().resideInAnyPackage(HTTP_CLIENTS)
                .check(classes);
    }
}

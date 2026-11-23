package com.geopslabs.geops.identity.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * US28: Identity does not invoke Notification. It does not use its classes and the only HTTP clients it has are the
 * adapters of its external providers (Nominatim and the SUNAT download); what Notification needs leaves as an event
 * through the outbox.
 */
class NotificationDecouplingArchTest {
    private static final String OWN_PACKAGE = "com.geopslabs.geops.identity";
    private static final String NOTIFICATION = "com.geopslabs.geops.notification..";
    private static final String[] PROVIDER_ADAPTERS = {
            OWN_PACKAGE + ".infrastructure.geocoding..", OWN_PACKAGE + ".infrastructure.sunat..",
            OWN_PACKAGE + ".infrastructure.decolecta.."
    };
    private static final String[] HTTP_CLIENTS = {
            "org.springframework.web.client..", "org.springframework.web.reactive.function.client..",
            "java.net.http..", "org.springframework.cloud.openfeign.."
    };

    private static JavaClasses classes;

    @BeforeAll
    static void importProductionClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(OWN_PACKAGE);
    }

    @Test
    void identityDoesNotDependOnNotification() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAPackage(NOTIFICATION)
                .because("Notification learns about accounts from identity.events (US28)")
                .check(classes);
    }

    @Test
    void onlyTheProviderAdaptersMakeHttpCalls() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..")
                .and().resideOutsideOfPackages(PROVIDER_ADAPTERS)
                .should().dependOnClassesThat().resideInAnyPackage(HTTP_CLIENTS)
                .because("Identity publishes events instead of calling other services synchronously (US28)")
                .check(classes);
    }

    @Test
    void theRegistrationWritesToTheOutboxAndNeverToKafkaDirectly() {
        noClasses().that().resideInAnyPackage(OWN_PACKAGE + "..application..", OWN_PACKAGE + "..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.kafka..")
                .because("the event is stored in the transaction of the use case and the relay publishes it")
                .check(classes);
    }
}

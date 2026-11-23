package com.geopslabs.geops.notification.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * US28: Notification does not depend on Identity code and does not call it. What it knows about an account comes
 * from identity.events, read by one listener; it has no HTTP client to reach another service.
 */
class IdentityDecouplingArchTest {
    private static final String OWN_PACKAGE = "com.geopslabs.geops.notification";
    private static final String IDENTITY = "com.geopslabs.geops.identity..";
    private static final String MESSAGING = OWN_PACKAGE + ".infrastructure.messaging..";
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
    void notificationDoesNotDependOnIdentityCode() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAPackage(IDENTITY)
                .because("the events of Identity are read from their JSON contract, not from its classes")
                .check(classes);
    }

    @Test
    void notificationMakesNoSynchronousCallToAnotherService() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAnyPackage(HTTP_CLIENTS)
                .because("Identity reaches Notification only through identity.events (US28)")
                .check(classes);
    }

    @Test
    void onlyTheMessagingAdaptersListenToTheEventBus() {
        classes().that().areAnnotatedWith("org.springframework.kafka.annotation.KafkaListener")
                .or().containAnyMethodsThat(com.tngtech.archunit.base.DescribedPredicate.describe(
                        "are annotated with @KafkaListener",
                        method -> method.isAnnotatedWith("org.springframework.kafka.annotation.KafkaListener")))
                .should().resideInAPackage(MESSAGING)
                .check(classes);
    }
}

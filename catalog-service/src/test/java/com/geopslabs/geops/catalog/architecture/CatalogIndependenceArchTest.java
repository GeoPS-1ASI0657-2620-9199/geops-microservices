package com.geopslabs.geops.catalog.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * US17 and US16: Catalog answers its searches without calling any other GeoPS service, and every outside technology
 * stays behind its adapter. The only outgoing HTTP client is the Nominatim adapter, an external provider behind a
 * circuit breaker (S2-03); other contexts reach Catalog through Kafka or its API, never the other way round.
 */
class CatalogIndependenceArchTest {
    private static final String OWN_PACKAGE = "com.geopslabs.geops.catalog";
    private static final String GEOCODING_ADAPTER = OWN_PACKAGE + ".infrastructure.geocoding..";
    private static final String CACHE_ADAPTER = OWN_PACKAGE + ".infrastructure.cache..";
    private static final String MESSAGING_ADAPTER = OWN_PACKAGE + ".infrastructure.messaging..";
    private static final String CONFIGURATION = OWN_PACKAGE + ".configuration..";

    private static JavaClasses classes;

    @BeforeAll
    static void importProductionClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(OWN_PACKAGE);
    }

    @Test
    void onlyTheNominatimAdapterMakesHttpCalls() {
        noClasses().that().resideOutsideOfPackage(GEOCODING_ADAPTER)
                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.web.client.RestClient")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.springframework.web.client.RestTemplate")
                .orShould().dependOnClassesThat().resideInAPackage("org.springframework.web.reactive.function.client..")
                .orShould().dependOnClassesThat().resideInAPackage("java.net.http..")
                .because("Catalog never waits on another GeoPS service to answer a search (US17, CA-17.2)")
                .check(classes);
    }

    @Test
    void domainAndApplicationDoNotKnowKafkaRedisNorHttp() {
        noClasses().that().resideInAnyPackage(OWN_PACKAGE + ".domain..", OWN_PACKAGE + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage("org.apache.kafka..", "org.springframework.kafka..",
                        "org.springframework.data.redis..", "io.lettuce..", "io.github.resilience4j..",
                        "org.springframework.web..")
                .check(classes);
    }

    @Test
    void domainIsPlainJava() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage("com.fasterxml.jackson..", "io.micrometer..",
                        "org.slf4j..", "jakarta..", "org.springframework..")
                .check(classes);
    }

    @Test
    void redisIsReachedOnlyThroughTheCellCacheAdapter() {
        noClasses().that().resideOutsideOfPackages(CACHE_ADAPTER, CONFIGURATION)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.data.redis..", "io.lettuce..")
                .check(classes);
    }

    @Test
    void kafkaIsReachedOnlyThroughTheMessagingAdapter() {
        noClasses().that().resideOutsideOfPackages(MESSAGING_ADAPTER, CONFIGURATION)
                .should().dependOnClassesThat().resideInAnyPackage("org.apache.kafka..", "org.springframework.kafka..")
                .check(classes);
    }
}

package com.geopslabs.geops.notification.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * US16: Notification refers to other bounded contexts only by identifier.
 */
class ContextBoundariesArchTest {
    private static final String ROOT_PACKAGE = "com.geopslabs.geops";
    private static final String OWN_PACKAGE = ROOT_PACKAGE + ".notification";
    private static final String[] OTHER_CONTEXTS = {
            ROOT_PACKAGE + ".identity..",
            ROOT_PACKAGE + ".catalog..",
            ROOT_PACKAGE + ".reservation..",
            ROOT_PACKAGE + ".engagement.."
    };

    private static JavaClasses classes;

    @BeforeAll
    static void importProductionClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(OWN_PACKAGE);
    }

    @Test
    void noClassDependsOnAnotherContext() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..")
                .should().dependOnClassesThat().resideInAnyPackage(OTHER_CONTEXTS)
                .because("other contexts are reached through their published API, never their classes")
                .check(classes);
    }

    @Test
    void domainDoesNotDependOnPersistenceOrSpring() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework..")
                .check(classes);
    }

    @Test
    void domainDoesNotDependOnOuterLayers() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        OWN_PACKAGE + "..application..", OWN_PACKAGE + "..infrastructure..",
                        OWN_PACKAGE + "..configuration..")
                .check(classes);
    }

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        noClasses().that().resideInAPackage(OWN_PACKAGE + "..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        OWN_PACKAGE + "..infrastructure..", OWN_PACKAGE + "..configuration..")
                .check(classes);
    }

    @Test
    void localCopiesOfOtherContextsAreNotMappedAsAssociations() {
        fields().that().areAnnotatedWith(ManyToOne.class)
                .or().areAnnotatedWith(OneToOne.class)
                .should(notMapSnapshotsOrProjections())
                .allowEmptyShould(true)
                .because("a snapshot or projection of another context is referenced by its identifier")
                .check(classes);
    }

    @Test
    void entityRelationshipsStayInsideTheContext() {
        fields().that().areAnnotatedWith(ManyToOne.class)
                .or().areAnnotatedWith(OneToMany.class)
                .or().areAnnotatedWith(OneToOne.class)
                .or().areAnnotatedWith(ManyToMany.class)
                .should(referToTypesOfThisContextOnly())
                .allowEmptyShould(true)
                .check(classes);
    }

    private static boolean isInsideOwnContext(String packageName) {
        return packageName.equals(OWN_PACKAGE) || packageName.startsWith(OWN_PACKAGE + ".");
    }

    private static ArchCondition<JavaField> notMapSnapshotsOrProjections() {
        return new ArchCondition<>("not map a snapshot or projection") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                field.getType().getAllInvolvedRawTypes().stream()
                        .filter(type -> type.getSimpleName().contains("Snapshot")
                                || type.getSimpleName().contains("Projection"))
                        .forEach(type -> events.add(SimpleConditionEvent.violated(field,
                                field.getFullName() + " maps " + type.getName() + " as an association")));
            }
        };
    }

    private static ArchCondition<JavaField> referToTypesOfThisContextOnly() {
        DescribedPredicate<JavaClass> outsideContext = DescribedPredicate.describe("outside " + OWN_PACKAGE,
                type -> type.getPackageName().startsWith(ROOT_PACKAGE + ".")
                        && !isInsideOwnContext(type.getPackageName()));
        return new ArchCondition<>("refer to types of this context only") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                field.getType().getAllInvolvedRawTypes().stream()
                        .filter(outsideContext)
                        .forEach(type -> events.add(SimpleConditionEvent.violated(field,
                                field.getFullName() + " maps " + type.getName() + " of another context")));
            }
        };
    }
}

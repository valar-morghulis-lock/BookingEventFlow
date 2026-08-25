package com.bookingeventflow.event.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.bookingeventflow.event")
class DomainArchitectureTest {

    private static final String DOMAIN_PACKAGE =
            "com.bookingeventflow.event.domain..";

    @ArchTest
    static final ArchRule domainMustNotDependOnController =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.controller.."
                    );

    @ArchTest
    static final ArchRule domainMustNotDependOnEntity =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.entity.."
                    );

    @ArchTest
    static final ArchRule domainMustNotDependOnRepository =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.repository.."
                    );

    @ArchTest
    static final ArchRule domainMustNotDependOnSpring =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.."
                    );

    @ArchTest
    static final ArchRule domainMustNotDependOnPersistence =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "jakarta.persistence..",
                            "org.hibernate.."
                    );

    @ArchTest
    static final ArchRule domainMustNotDependOnKafka =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAIN_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.kafka.."
                    );
}
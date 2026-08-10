package com.bookingeventflow.event.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


@AnalyzeClasses(packages = "com.bookingeventflow.event")
class SharedArchitectureTest {


    private static final String SHARED_PACKAGE =
            "com.bookingeventflow.event.shared..";

    @ArchTest
    static final ArchRule sharedMustNotDependOnApi =
            noClasses()
                    .that()
                    .resideInAnyPackage(SHARED_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.api.."
                    );

    @ArchTest
    static final ArchRule sharedMustNotDependOnApplication =
            noClasses()
                    .that()
                    .resideInAnyPackage(SHARED_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.application.."
                    );

    @ArchTest
    static final ArchRule sharedMustNotDependOnDomain =
            noClasses()
                    .that()
                    .resideInAnyPackage(SHARED_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.domain.."
                    );

    @ArchTest
    static final ArchRule sharedMustNotDependOnInfrastructure =
            noClasses()
                    .that()
                    .resideInAnyPackage(SHARED_PACKAGE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.infrastructure.."
                    );


}

package com.bookingeventflow.event.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.bookingeventflow.event")
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule controllerMustNotBeAccessedByAnyOtherPackage =
            noClasses()
                    .that()
                    .resideOutsideOfPackage(
                            "com.bookingeventflow.event.controller.."
                    )
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.controller.."
                    );

    @ArchTest
    static final ArchRule repositoriesMustNotBeAccessedByControllers =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.controller.."
                    )
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.repository..",
                            "com.bookingeventflow.event.outbox.repository.."
                    );

    @ArchTest
    static final ArchRule entitiesMustNotBeAccessedByControllers =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.controller.."
                    )
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.event.entity..",
                            "com.bookingeventflow.event.outbox.entity.."
                    );
}
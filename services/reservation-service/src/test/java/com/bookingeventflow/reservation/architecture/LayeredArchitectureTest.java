package com.bookingeventflow.reservation.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.bookingeventflow.reservation")
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule controllerMustNotBeAccessedByAnyOtherPackage =
            noClasses()
                    .that()
                    .resideOutsideOfPackage(
                            "com.bookingeventflow.reservation.controller.."
                    )
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.reservation.controller.."
                    );

    @ArchTest
    static final ArchRule repositoriesMustNotBeAccessedByControllers =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "com.bookingeventflow.reservation.controller.."
                    )
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.reservation.repository.."
                    );

    @ArchTest
    static final ArchRule entitiesMustNotBeAccessedByControllers =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "com.bookingeventflow.reservation.controller.."
                    )
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.bookingeventflow.reservation.entity.."
                    );
}
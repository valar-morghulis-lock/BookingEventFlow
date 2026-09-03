package com.bookingeventflow.customer.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.bookingeventflow.customer")
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule controllerMustNotBeAccessedByAnyOtherPackage =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("com.bookingeventflow.customer.controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("com.bookingeventflow.customer.controller..");

    @ArchTest
    static final ArchRule repositoriesMustNotBeAccessedByControllers =
            noClasses()
                    .that()
                    .resideInAnyPackage("com.bookingeventflow.customer.controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("com.bookingeventflow.customer.repository..");
}
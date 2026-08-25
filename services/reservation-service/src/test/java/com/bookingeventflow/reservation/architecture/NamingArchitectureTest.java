package com.bookingeventflow.reservation.architecture;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.bookingeventflow.reservation")
public class NamingArchitectureTest {

    @ArchTest
    static final ArchRule services_should_have_service_suffix =
            classes()
                    .that()
                    .resideInAnyPackage("..service..")
                    .should()
                    .haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule repositories_should_have_repository_suffix =
            classes()
                    .that()
                    .resideInAnyPackage("..repository..")
                    .should()
                    .haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule consumers_should_have_consumer_suffix =
            classes()
                    .that()
                    .resideInAnyPackage("..messaging.consumer..")
                    .should()
                    .haveSimpleNameEndingWith("Consumer");

    @ArchTest
    static final ArchRule entities_should_have_entity_suffix =
            classes()
                    .that()
                    .resideInAnyPackage("..entity..")
                    .should()
                    .haveSimpleNameEndingWith("Entity");

    @ArchTest
    static final ArchRule services_should_not_have_impl_suffix =
            noClasses()
                    .that()
                    .resideInAnyPackage("..service..")
                    .should()
                    .haveSimpleNameEndingWith("Impl");

    @ArchTest
    static final ArchRule repositories_should_not_have_impl_suffix =
            noClasses()
                    .that()
                    .resideInAnyPackage("..repository..")
                    .should()
                    .haveSimpleNameEndingWith("Impl");
}
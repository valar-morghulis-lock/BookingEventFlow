package com.bookingeventflow.event.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.bookingeventflow.event")
class LayeredArchitectureTest {


    @ArchTest
    static final ArchRule layeredArchitecture = layeredArchitecture().consideringAllDependencies()

            .layer("API").definedBy("com.bookingeventflow.event.api..")

            .layer("APPLICATION").definedBy("com.bookingeventflow.event.application..")

            .layer("DOMAIN").definedBy("com.bookingeventflow.event.domain..")

            .layer("INFRASTRUCTURE").definedBy("com.bookingeventflow.event.infrastructure..")

            /*
             * API is an entry point.
             */.whereLayer("API").mayNotBeAccessedByAnyLayer()

            /*
             * Application is an orchestration layer.
             * API may call it, but it must not depend on
             * API or infrastructure.
             */.whereLayer("APPLICATION").mayOnlyBeAccessedByLayers("API")

            /*
             * Domain is the business core.
             */.whereLayer("DOMAIN").mayOnlyBeAccessedByLayers("APPLICATION", "INFRASTRUCTURE")

            /*
             * Infrastructure contains implementations
             * of technical concerns.
             */.whereLayer("INFRASTRUCTURE").mayNotBeAccessedByAnyLayer();


}

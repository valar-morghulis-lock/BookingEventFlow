package com.bookingeventflow.common.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {

        /*
         * Phase 1
         * Always return SYSTEM.
         *
         * Phase 2
         * Read the authenticated user from Spring Security.
         *
         * Phase 3
         * Read the JWT subject from Keycloak.
         */

        return Optional.of(AuditConstants.SYSTEM);
    }

}
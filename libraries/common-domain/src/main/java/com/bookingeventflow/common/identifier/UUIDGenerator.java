package com.bookingeventflow.common.identifier;

import java.util.UUID;

public final class UUIDGenerator implements IdentifierGenerator<UUID> {

    public static final UUIDGenerator INSTANCE = new UUIDGenerator();

    private UUIDGenerator() {
    }

    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
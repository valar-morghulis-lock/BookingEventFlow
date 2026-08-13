package com.bookingeventflow.common.entity;

import com.bookingeventflow.common.identifier.UUIDGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Version
    @Column(
            nullable = false
    )
    private Long version;

    protected BaseEntity() {
        /*
         * New entities receive their UUID immediately.
         *
         * IMPORTANT:
         * version intentionally remains null.
         *
         * Hibernate uses the null version to recognize this
         * as a new entity when Spring Data calls save().
         */
        this.id = UUIDGenerator.INSTANCE.generate();
    }

    /**
     * Used when reconstructing an entity whose identity and
     * optimistic-lock version already exist in the database.
     */
    protected void reconstituteIdentity(
            UUID id,
            Long version
    ) {
        this.id = Objects.requireNonNull(
                id,
                "Entity id must not be null"
        );

        this.version = Objects.requireNonNull(
                version,
                "Entity version must not be null"
        );
    }

    public UUID id() {
        return id;
    }

    public Long version() {
        return version;
    }

    @Override
    public final boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        BaseEntity other = (BaseEntity) object;

        return id != null && id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
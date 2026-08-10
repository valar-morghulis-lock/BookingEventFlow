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
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version;

    protected BaseEntity() {
        this.id = UUIDGenerator.INSTANCE.generate();
    }

    /**
     * Returns the unique identifier of this entity.
     */
    public UUID id() {
        return id;
    }

    /**
     * Returns the JPA optimistic-lock version.
     */
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
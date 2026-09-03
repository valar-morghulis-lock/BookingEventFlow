package com.bookingeventflow.customer.entity;

import com.bookingeventflow.common.entity.AuditableEntity;
import com.bookingeventflow.customer.domain.model.CustomerRole;
import com.bookingeventflow.customer.domain.model.CustomerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "customers",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_customers_email",
                columnNames = "email"
        )
)
public class CustomerEntity extends AuditableEntity {

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerRole role;

    protected CustomerEntity() {
        // Required by JPA.
    }

    public CustomerEntity(
            String email,
            String passwordHash,
            String firstName,
            String lastName
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = CustomerStatus.ACTIVE;
        this.role = CustomerRole.CUSTOMER;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public CustomerRole getRole() {
        return role;
    }

    public void setRole(CustomerRole role) {
        this.role = role;
    }
}
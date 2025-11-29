package org.hmmk.sms.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@MappedSuperclass
public abstract class TenantScopedEntity extends PanacheEntityBase {

    @Column(nullable = false)
    public String tenantId;

    @CreationTimestamp
    public Instant createdAt;

    @UpdateTimestamp
    public Instant updatedAt;
}

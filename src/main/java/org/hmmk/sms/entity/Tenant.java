package org.hmmk.sms.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    public String id;

    @Column(nullable = false, unique = true, length = 150)
    public String name;

    @Column(unique = true, length = 200)
    public String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TenantStatus status;

    @Column(name = "sms_credit", nullable = false)
    public long smsCredit;// total SMS available

    @Column(name = "config_json", columnDefinition = "TEXT")
    public String configJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum TenantStatus {
        ACTIVE,
        INACTIVE
    }
}

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
    public String id;

    @Column(nullable = false, unique = true, length = 150)
    public String name;

    @Column(length = 255)
    public String email;

    @Column(length = 20)
    public String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TenantStatus status;

    @Column(name = "is_company", nullable = false)
    public boolean isCompany;

    @Column(name = "tin_number", length = 30)
    public String tinNumber;

    @Column(length = 1000)
    public String description;

    @Column(name = "sms_credit", nullable = false)
    public long smsCredit;// total SMS available

    @Builder.Default
    @Column(name = "sms_approval_threshold", nullable = false)
    public int smsApprovalThreshold = 100; // Number of recipients above which an SMS job requires admin approval

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

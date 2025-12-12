package org.hmmk.sms.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hmmk.sms.entity.Sender;
import org.hmmk.sms.entity.TenantScopedEntity;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    public Sender sender;

    @Column(name = "api_key", nullable = false, unique = true)
    public String apiKey;

    public String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        if (this.apiKey == null) {
            this.apiKey = generateApiKey();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public static String generateApiKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

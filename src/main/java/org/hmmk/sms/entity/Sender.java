package org.hmmk.sms.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "sender",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenantId", "short_code"})
        }
)
public class Sender extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    public String name;

    @Column(name = "short_code", nullable = false,  length = 200)
    public String shortCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Sender.SenderStatus status;

    public enum SenderStatus {
        ACTIVE,
        INACTIVE,
        PENDING_VERIFICATION,
        REJECTED
    }
}

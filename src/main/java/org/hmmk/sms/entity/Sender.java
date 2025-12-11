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
@Table(name = "sender")
public class Sender extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    public String name;

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

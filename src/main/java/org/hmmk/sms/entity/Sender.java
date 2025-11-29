package org.hmmk.sms.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sender")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sender extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @Column(nullable = false)
    public String tenantId;

    public String name;

    @Column(name = "short_code", nullable = false, unique = true, length = 200)
    public String shortCode;;

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

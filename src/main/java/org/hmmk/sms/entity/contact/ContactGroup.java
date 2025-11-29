package org.hmmk.sms.entity.contact;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hmmk.sms.entity.TenantScopedEntity;

import java.time.Instant;

@Entity
@Table(name = "group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactGroup extends TenantScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;
    @Column(nullable = false)
    public String tenantId;
    @Column(nullable = false)
    public String name;
    public String description;

}

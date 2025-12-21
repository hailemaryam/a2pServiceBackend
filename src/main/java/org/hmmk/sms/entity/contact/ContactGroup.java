package org.hmmk.sms.entity.contact;

import jakarta.persistence.*;
import lombok.*;
import org.hmmk.sms.entity.TenantScopedEntity;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contact_groups")
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

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ContactGroupMember> members = new HashSet<>();

}

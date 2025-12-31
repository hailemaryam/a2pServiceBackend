package org.hmmk.sms.entity.contact;

import jakarta.persistence.*;
import lombok.*;
import org.hmmk.sms.entity.TenantScopedEntity;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contacts", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "tenantId", "phone" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact extends TenantScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;
    @Column(nullable = false)
    public String phone;
    public String name;
    public String email;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ContactGroupMember> groupMembers = new HashSet<>();
}

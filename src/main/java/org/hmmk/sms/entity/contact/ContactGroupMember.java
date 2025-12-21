package org.hmmk.sms.entity.contact;

import jakarta.persistence.*;
import lombok.*;
import org.hmmk.sms.entity.TenantScopedEntity;

@Entity
@Table(name = "contact_group_members")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ContactGroupMember  extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "group_id")
    private ContactGroup group;
}

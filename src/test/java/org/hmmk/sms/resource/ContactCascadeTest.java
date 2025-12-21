package org.hmmk.sms.resource;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.hmmk.sms.entity.ApiKey;
import org.hmmk.sms.entity.Sender;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.contact.Contact;
import org.hmmk.sms.entity.contact.ContactGroup;
import org.hmmk.sms.entity.contact.ContactGroupMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ContactCascadeTest {

    @BeforeEach
    @Transactional
    public void cleanup() {
        // clear in child->parent order
        ContactGroupMember.deleteAll();
        ContactGroup.deleteAll();
        Contact.deleteAll();
        ApiKey.deleteAll();
        Tenant.deleteAll();
    }

    @Test
    @Transactional
    public void testCascadeOnContactDelete() {
        // create tenant
        Tenant tenant = new Tenant();
        tenant.name = "Cascade Tenant" + System.currentTimeMillis();
        tenant.phone = "+000000000";
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();

        // create group
        ContactGroup group = new ContactGroup();
        group.tenantId = tenant.id;
        group.name = "G1";
        group.persist();

        // create contact
        Contact contact = new Contact();
        contact.tenantId = tenant.id;
        contact.phone = "+1111111111";
        contact.persist();

        // create member
        ContactGroupMember member = new ContactGroupMember();
        member.tenantId = tenant.id;
        member.setContact(contact);
        member.setGroup(group);
        member.persist();

        assertEquals(1, ContactGroupMember.count(), "One member should exist");

        // delete contact
        contact.delete();

        assertEquals(0, ContactGroupMember.count(), "Member should be deleted when contact is deleted");
        assertNull(Contact.findById(contact.id));
    }

    @Test
    @Transactional
    public void testCascadeOnGroupDelete() {
        // create tenant
        Tenant tenant = new Tenant();
        tenant.name = "Cascade Tenant" + System.currentTimeMillis();
        tenant.phone = "+000000001";
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();

        // create group
        ContactGroup group = new ContactGroup();
        group.tenantId = tenant.id;
        group.name = "G2";
        group.persist();

        // create contact
        Contact contact = new Contact();
        contact.tenantId = tenant.id;
        contact.phone = "+2222222222";
        contact.persist();

        // create member
        ContactGroupMember member = new ContactGroupMember();
        member.tenantId = tenant.id;
        member.setContact(contact);
        member.setGroup(group);
        member.persist();

        assertEquals(1, ContactGroupMember.count(), "One member should exist");

        // delete group
        group.delete();

        assertEquals(0, ContactGroupMember.count(), "Member should be deleted when group is deleted");
        assertNull(ContactGroup.findById(group.id));
    }
}

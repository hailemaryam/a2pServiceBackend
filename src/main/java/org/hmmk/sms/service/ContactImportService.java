package org.hmmk.sms.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.hmmk.sms.entity.contact.Contact;
import org.hmmk.sms.entity.contact.ContactGroup;
import org.hmmk.sms.entity.contact.ContactGroupMember;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ContactImportService {

    @Transactional
    public List<Contact> importFromCsv(InputStream csvStream, String tenantId, String groupId) throws Exception {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("tenantId missing from JWT");
        }

        List<Contact> result = new ArrayList<>();

        ContactGroup group = null;
        if (groupId != null && !groupId.isBlank()) {
            group = ContactGroup.findById(groupId);
            if (group == null) {
                throw new NotFoundException("ContactGroup not found: " + groupId);
            }
            // ensure group belongs to tenant
            if (group.tenantId == null || !group.tenantId.equals(tenantId)) {
                throw new NotFoundException("ContactGroup not found for tenant: " + groupId);
            }
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            // expecting header: phone,name,email
            String header = br.readLine();
            if (header == null) return result;
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String phone = parts.length > 0 ? parts[0].trim() : null;
                String name = parts.length > 1 ? parts[1].trim() : null;
                String email = parts.length > 2 ? parts[2].trim() : null;
                if (phone == null || phone.isBlank()) continue;

                // find existing contact by tenant and phone
                Contact c = Contact.find("tenantId = ?1 and phone = ?2", tenantId, phone).firstResult();

                if (c == null) {
                    // create new contact
                    c = new Contact();
                    c.phone = phone;
                    c.name = name;
                    c.email = email;
                    c.tenantId = tenantId;
                    c.persist();
                }

                // if a group was provided, create a ContactGroupMember linking them (avoid duplicates)
                if (group != null) {
                    ContactGroupMember existing = ContactGroupMember.find("group = ?1 and contact = ?2", group, c).firstResult();
                    if (existing == null) {
                        ContactGroupMember member = new ContactGroupMember();
                        member.setContact(c);
                        member.setGroup(group);
                        member.tenantId = tenantId;
                        member.persist();
                    }
                }

                result.add(c);
            }
        }
        return result;
    }
}

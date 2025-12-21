package org.hmmk.sms.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.hmmk.sms.entity.contact.Contact;
import org.hmmk.sms.entity.contact.ContactGroup;
import org.hmmk.sms.entity.contact.ContactGroupMember;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ContactImportService {

    @Transactional
    public void addContactsToGroup(String tenantId, String groupId, List<String> contactIds) {
        ContactGroup group = ContactGroup.findById(groupId);
        if (group == null || group.tenantId == null || !group.tenantId.equals(tenantId)) {
            throw new BadRequestException("Invalid groupId");
        }
        for (String contactId : contactIds) {
            Contact contact = Contact.findById(contactId);
            if (contact != null && contact.tenantId != null && contact.tenantId.equals(tenantId)) {
                ContactGroupMember existing = ContactGroupMember.find("group = ?1 and contact = ?2 and tenantId = ?3", group, contact, tenantId).firstResult();
                if (existing == null) {
                    ContactGroupMember member = new ContactGroupMember();
                    member.setContact(contact);
                    member.setGroup(group);
                    member.tenantId = tenantId;
                    member.persist();
                }
            }
        }
    }

    @Transactional
    public List<Contact> importFromCsv(InputStream csvStream, String tenantId, String groupId) throws Exception {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("tenantId missing from JWT");
        }

        // Buffer the stream so we can try Excel then fallback to CSV
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = csvStream.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        byte[] bytes = baos.toByteArray();

        ContactGroup group = null;
        if (groupId != null && !groupId.isBlank()) {
            ContactGroup resolved = ContactGroup.findById(groupId);
            if (resolved != null && resolved.tenantId != null && resolved.tenantId.equals(tenantId)) {
                group = resolved;
            }
        }

        List<Contact> result = new ArrayList<>();

        // First try Excel parsing
        boolean parsed = false;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            try (Workbook workbook = WorkbookFactory.create(bais)) {
                if (workbook.getNumberOfSheets() > 0) {
                    Sheet sheet = workbook.getSheetAt(0);
                    for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;
                        // read cells
                        String phone = getCellString(row, 0);
                        String name = getCellString(row, 1);
                        String email = getCellString(row, 2);
                        if (phone == null || phone.isBlank()) continue;
                        processContactRow(result, tenantId, group, phone.trim(), (name == null ? null : name.trim()), (email == null ? null : email.trim()));
                    }
                    parsed = true;
                }
            } catch (Exception e) {
                // Not an Excel file or failed to parse; fall through to CSV
                parsed = false;
            }
        }

        if (!parsed) {
            // Parse as CSV using an internal parser to avoid external dependency
            try (ByteArrayInputStream bais2 = new ByteArrayInputStream(bytes);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(bais2, StandardCharsets.UTF_8))) {
                String header = reader.readLine(); // skip header if present
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] cols = parseCsvLine(line);
                    String phone = cols.length > 0 ? cols[0] : null;
                    String name = cols.length > 1 ? cols[1] : null;
                    String email = cols.length > 2 ? cols[2] : null;
                    if (phone == null || phone.isBlank()) continue;
                    processContactRow(result, tenantId, group, phone.trim(), (name == null ? null : name.trim()), (email == null ? null : email.trim()));
                }
            }
        }

        return result;
    }

    private void processContactRow(List<Contact> result, String tenantId, ContactGroup group, String phone, String name, String email) {
        Contact c = Contact.find("tenantId = ?1 and phone = ?2", tenantId, phone).firstResult();
        if (c == null) {
            c = new Contact();
            c.phone = phone;
            c.name = name;
            c.email = email;
            c.tenantId = tenantId;
            c.persist();
        }
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

    private String getCellString(Row row, int col) {
        if (row.getCell(col) == null) return null;
        try {
            return row.getCell(col).getStringCellValue();
        } catch (Exception e) {
            try {
                double d = row.getCell(col).getNumericCellValue();
                if (d == (long) d) return String.valueOf((long) d);
                return String.valueOf(d);
            } catch (Exception ex) {
                return row.getCell(col).toString();
            }
        }
    }

    // Simple CSV line parser that supports quoted fields and escaped quotes
    private String[] parseCsvLine(String line) {
        if (line == null) return new String[0];
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        // escaped quote
                        cur.append('"');
                        i++; // skip next
                    } else {
                        inQuotes = false; // end quote
                    }
                } else {
                    cur.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    cols.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(ch);
                }
            }
        }
        cols.add(cur.toString());
        // trim entries
        for (int i = 0; i < cols.size(); i++) {
            cols.set(i, cols.get(i).trim());
        }
        return cols.toArray(new String[0]);
    }
}

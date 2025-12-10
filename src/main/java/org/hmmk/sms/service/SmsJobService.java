package org.hmmk.sms.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hmmk.sms.dto.sms.BulkSmsRequest;
import org.hmmk.sms.dto.sms.GroupSmsRequest;
import org.hmmk.sms.dto.sms.SingleSmsRequest;
import org.hmmk.sms.entity.Sender;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.contact.Contact;
import org.hmmk.sms.entity.contact.ContactGroup;
import org.hmmk.sms.entity.contact.ContactGroupMember;
import org.hmmk.sms.entity.sms.SmsJob;
import org.hmmk.sms.entity.sms.SmsRecipient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for creating and managing SMS jobs.
 */
@ApplicationScoped
public class SmsJobService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");

    /**
     * Creates a single SMS job for one recipient.
     */
    @Transactional
    public SmsJob sendSingle(String tenantId, String userId, SingleSmsRequest req) {
        validateSender(tenantId, req.getSenderId());
        Tenant tenant = getTenant(tenantId);

        SmsJob.MessageType messageType = detectMessageType(req.getMessage());
        int smsCount = calculateSmsCount(req.getMessage(), messageType);

        // Validate sufficient credits (single SMS is always auto-approved)
        validateSufficientCredits(tenant, smsCount);

        SmsJob job = SmsJob.builder()
                .senderId(req.getSenderId())
                .jobType(SmsJob.JobType.SINGLE)
                .sourceType(SmsJob.SourceType.MANUAL)
                .messageContent(req.getMessage())
                .messageType(messageType)
                .totalRecipients(1L)
                .totalSmsCount((long) smsCount)
                .createdBy(userId)
                .scheduledAt(req.getScheduledAt() != null ? req.getScheduledAt() : Instant.now())
                .approvalStatus(SmsJob.ApprovalStatus.APPROVED) // Single SMS always approved
                .status(SmsJob.JobStatus.SCHEDULED)
                .build();
        job.tenantId = tenantId;

        job.persist();

        // Deduct credits immediately for auto-approved job
        deductCredits(tenant, smsCount);

        // Create recipient
        createRecipient(job, req.getPhoneNumber(), req.getMessage(), messageType, tenantId);

        return job;
    }

    /**
     * Creates a group SMS job for all contacts in a contact group.
     */
    @Transactional
    public SmsJob sendToGroup(String tenantId, String userId, GroupSmsRequest req) {
        validateSender(tenantId, req.getSenderId());
        Tenant tenant = getTenant(tenantId);

        // Validate contact group exists and belongs to tenant
        ContactGroup group = ContactGroup.findById(req.getGroupId());
        if (group == null || !tenantId.equals(group.tenantId)) {
            throw new NotFoundException("Contact group not found");
        }

        // Get all contacts in the group
        List<ContactGroupMember> members = ContactGroupMember.list("group.id", req.getGroupId());
        if (members.isEmpty()) {
            throw new BadRequestException("Contact group has no members");
        }

        SmsJob.MessageType messageType = detectMessageType(req.getMessage());
        int smsCountPerRecipient = calculateSmsCount(req.getMessage(), messageType);
        long totalRecipients = members.size();
        long totalSmsCount = totalRecipients * smsCountPerRecipient;

        // Check if approval is required
        boolean requiresApproval = totalRecipients > tenant.smsApprovalThreshold;

        // Validate sufficient credits
        validateSufficientCredits(tenant, totalSmsCount);

        SmsJob job = SmsJob.builder()
                .senderId(req.getSenderId())
                .jobType(SmsJob.JobType.GROUP)
                .sourceType(SmsJob.SourceType.MANUAL)
                .messageContent(req.getMessage())
                .messageType(messageType)
                .totalRecipients(totalRecipients)
                .totalSmsCount(totalSmsCount)
                .createdBy(userId)
                .scheduledAt(req.getScheduledAt() != null ? req.getScheduledAt() : Instant.now())
                .groupId(req.getGroupId())
                .approvalStatus(requiresApproval ? SmsJob.ApprovalStatus.PENDING : SmsJob.ApprovalStatus.APPROVED)
                .status(requiresApproval ? SmsJob.JobStatus.PENDING_APPROVAL : SmsJob.JobStatus.SCHEDULED)
                .build();
        job.tenantId = tenantId;

        job.persist();

        // Deduct credits immediately if auto-approved
        if (!requiresApproval) {
            deductCredits(tenant, totalSmsCount);
        }

        // Create recipients for each group member
        for (ContactGroupMember member : members) {
            Contact contact = member.getContact();
            createRecipient(job, contact.phone, req.getMessage(), messageType, tenantId);
        }

        return job;
    }

    /**
     * Creates a bulk SMS job from a CSV file containing phone numbers.
     */
    @Transactional
    public SmsJob sendBulk(String tenantId, String userId, InputStream csvInputStream, BulkSmsRequest req) {
        validateSender(tenantId, req.getSenderId());
        Tenant tenant = getTenant(tenantId);

        // Parse phone numbers from CSV
        List<String> phoneNumbers = parsePhoneNumbersFromCsv(csvInputStream);
        if (phoneNumbers.isEmpty()) {
            throw new BadRequestException("CSV file contains no valid phone numbers");
        }

        SmsJob.MessageType messageType = detectMessageType(req.getMessage());
        int smsCountPerRecipient = calculateSmsCount(req.getMessage(), messageType);
        long totalRecipients = phoneNumbers.size();
        long totalSmsCount = totalRecipients * smsCountPerRecipient;

        // Check if approval is required
        boolean requiresApproval = totalRecipients > tenant.smsApprovalThreshold;

        // Validate sufficient credits
        validateSufficientCredits(tenant, totalSmsCount);

        SmsJob job = SmsJob.builder()
                .senderId(req.getSenderId())
                .jobType(SmsJob.JobType.BULK)
                .sourceType(SmsJob.SourceType.CSV_UPLOAD)
                .messageContent(req.getMessage())
                .messageType(messageType)
                .totalRecipients(totalRecipients)
                .totalSmsCount(totalSmsCount)
                .createdBy(userId)
                .scheduledAt(req.getScheduledAt() != null ? req.getScheduledAt() : Instant.now())
                .approvalStatus(requiresApproval ? SmsJob.ApprovalStatus.PENDING : SmsJob.ApprovalStatus.APPROVED)
                .status(requiresApproval ? SmsJob.JobStatus.PENDING_APPROVAL : SmsJob.JobStatus.SCHEDULED)
                .build();
        job.tenantId = tenantId;

        job.persist();

        // Deduct credits immediately if auto-approved
        if (!requiresApproval) {
            deductCredits(tenant, totalSmsCount);
        }

        // Create recipients for each phone number
        for (String phoneNumber : phoneNumbers) {
            createRecipient(job, phoneNumber, req.getMessage(), messageType, tenantId);
        }

        return job;
    }

    /**
     * Detects whether the message contains Unicode characters.
     */
    private SmsJob.MessageType detectMessageType(String message) {
        for (char c : message.toCharArray()) {
            if (c > 127) {
                return SmsJob.MessageType.UNICODE;
            }
        }
        return SmsJob.MessageType.English;
    }

    /**
     * Calculates the number of SMS segments required for a message.
     * English SMS: 160 chars for single, 153 chars per segment for multipart
     * Unicode SMS: 70 chars for single, 67 chars per segment for multipart
     */
    private int calculateSmsCount(String message, SmsJob.MessageType messageType) {
        int length = message.length();
        if (messageType == SmsJob.MessageType.UNICODE) {
            if (length <= 70)
                return 1;
            return (int) Math.ceil((double) length / 67);
        } else {
            if (length <= 160)
                return 1;
            return (int) Math.ceil((double) length / 153);
        }
    }

    /**
     * Creates a single SmsRecipient record.
     */
    private void createRecipient(SmsJob job, String phoneNumber, String message,
            SmsJob.MessageType messageType, String tenantId) {
        SmsRecipient recipient = SmsRecipient.builder()
                .senderId(job.senderId)
                .job(job)
                .phoneNumber(phoneNumber)
                .message(message)
                .messageType(messageType)
                .status(SmsRecipient.RecipientStatus.PENDING)
                .build();
        recipient.tenantId = tenantId;
        recipient.persist();
    }

    /**
     * Parses phone numbers from a CSV file.
     * Expects phone numbers in the first column (with or without header).
     */
    private List<String> parsePhoneNumbersFromCsv(InputStream inputStream) {
        List<String> phoneNumbers = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setSkipHeaderRecord(false)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            for (CSVRecord record : parser) {
                if (record.size() > 0) {
                    String phoneNumber = record.get(0).trim();
                    // Skip header row if it looks like a header
                    if (phoneNumber.equalsIgnoreCase("phone") ||
                            phoneNumber.equalsIgnoreCase("phonenumber") ||
                            phoneNumber.equalsIgnoreCase("phone_number") ||
                            phoneNumber.equalsIgnoreCase("mobile")) {
                        continue;
                    }
                    // Validate phone number format
                    if (PHONE_PATTERN.matcher(phoneNumber).matches()) {
                        phoneNumbers.add(phoneNumber);
                    }
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to parse CSV file: " + e.getMessage());
        }

        return phoneNumbers;
    }

    /**
     * Validates that the sender belongs to the tenant and is active.
     */
    private void validateSender(String tenantId, String senderId) {
        Sender sender = Sender.find("id = ?1 and tenantId = ?2", senderId, tenantId).firstResult();
        if (sender == null) {
            throw new NotFoundException("Sender not found");
        }
        if (sender.status != Sender.SenderStatus.ACTIVE) {
            throw new BadRequestException("Sender is not active");
        }
    }

    /**
     * Gets the tenant by ID.
     */
    private Tenant getTenant(String tenantId) {
        Tenant tenant = Tenant.findById(tenantId);
        if (tenant == null) {
            throw new NotFoundException("Tenant not found");
        }
        return tenant;
    }

    /**
     * Validates that the tenant has sufficient SMS credits.
     */
    private void validateSufficientCredits(Tenant tenant, long requiredCredits) {
        if (tenant.smsCredit < requiredCredits) {
            throw new BadRequestException(
                    String.format("Insufficient SMS credits. Required: %d, Available: %d",
                            requiredCredits, tenant.smsCredit));
        }
    }

    /**
     * Deducts SMS credits from the tenant's balance.
     */
    private void deductCredits(Tenant tenant, long credits) {
        tenant.smsCredit -= credits;
        tenant.persist();
    }

    /**
     * Approves an SMS job that is pending approval.
     * Changes the job status to SCHEDULED and records the approver.
     */
    @Transactional
    public SmsJob approveJob(UUID jobId, String adminUserId) {
        SmsJob job = SmsJob.findById(jobId);
        if (job == null) {
            throw new NotFoundException("SMS job not found");
        }

        if (job.approvalStatus != SmsJob.ApprovalStatus.PENDING) {
            throw new BadRequestException("Job is not pending approval");
        }

        // Get tenant and validate/deduct credits
        Tenant tenant = getTenant(job.tenantId);
        validateSufficientCredits(tenant, job.totalSmsCount);
        deductCredits(tenant, job.totalSmsCount);

        job.approvalStatus = SmsJob.ApprovalStatus.APPROVED;
        job.status = SmsJob.JobStatus.SCHEDULED;
        job.approvedBy = adminUserId;
        job.approvedAt = Instant.now();
        job.persist();

        return job;
    }

    /**
     * Rejects an SMS job that is pending approval.
     * Changes the approval status to REJECTED and records the rejector.
     */
    @Transactional
    public SmsJob rejectJob(UUID jobId, String adminUserId, String reason) {
        SmsJob job = SmsJob.findById(jobId);
        if (job == null) {
            throw new NotFoundException("SMS job not found");
        }

        if (job.approvalStatus != SmsJob.ApprovalStatus.PENDING) {
            throw new BadRequestException("Job is not pending approval");
        }

        job.approvalStatus = SmsJob.ApprovalStatus.REJECTED;
        job.status = SmsJob.JobStatus.FAILED;
        job.approvedBy = adminUserId; // Records who rejected it
        job.approvedAt = Instant.now();
        job.persist();

        return job;
    }

    /**
     * Lists SMS jobs pending approval with pagination.
     */
    public PaginatedResult<SmsJob> listPendingApprovalJobs(int page, int size) {
        var query = SmsJob.find("approvalStatus", SmsJob.ApprovalStatus.PENDING)
                .page(io.quarkus.panache.common.Page.of(page, size));
        List<SmsJob> items = query.list();
        long total = SmsJob.count("approvalStatus", SmsJob.ApprovalStatus.PENDING);
        return new PaginatedResult<>(items, total, page, size);
    }

    /**
     * Simple result wrapper for pagination in service layer.
     */
    public record PaginatedResult<T>(List<T> items, long total, int page, int size) {
    }

    /**
     * Gets an SMS job by ID.
     */
    public SmsJob getJobById(UUID jobId) {
        SmsJob job = SmsJob.findById(jobId);
        if (job == null) {
            throw new NotFoundException("SMS job not found");
        }
        return job;
    }
}

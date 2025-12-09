package org.hmmk.sms.entity.sms;

import jakarta.persistence.*;
import lombok.*;
import org.hmmk.sms.entity.TenantScopedEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sms_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsJob extends TenantScopedEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(nullable = false)
    public String senderId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public JobType jobType; // SINGLE, GROUP, BULK
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SourceType sourceType; // MANUAL, CSV_UPLOAD
    @Column(name = "message_content", nullable = false)
    public String messageContent;
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    public MessageType messageType; // English, UNICODE
    public Long totalRecipients;
    public Long totalSmsCount;
    @Column(name = "created_by", nullable = false)
    public String createdBy; // Keycloak user ID
    public Instant scheduledAt;
    @Enumerated(EnumType.STRING)
    public JobStatus status;
    @Column(name = "approved_by")
    public String approvedBy;
    public Instant approvedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ApprovalStatus approvalStatus;

    @Column(name = "group_id")
    public String groupId; // ContactGroup ID for JobType.GROUP

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum JobType {
        SINGLE, GROUP, BULK
    }

    public enum SourceType {
        API, MANUAL, CSV_UPLOAD
    }

    public enum JobStatus {
        PENDING_APPROVAL, SCHEDULED, SENDING, COMPLETED, FAILED
    }

    public enum MessageType {
        English, UNICODE
    } // can be detected automatically based on content

    @PrePersist
    public void prePersist() {
        if (status == null)
            status = JobStatus.PENDING_APPROVAL;
    }

}

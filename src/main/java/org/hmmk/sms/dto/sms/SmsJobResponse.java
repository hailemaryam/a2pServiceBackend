package org.hmmk.sms.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hmmk.sms.entity.sms.SmsJob;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for SMS job operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsJobResponse {

    private UUID id;
    private SmsJob.JobType jobType;
    private SmsJob.JobStatus status;
    private SmsJob.ApprovalStatus approvalStatus;
    private Long totalRecipients;
    private Long totalSmsCount;
    private Instant scheduledAt;
    private Instant createdAt;
    private String message;

    /**
     * Creates a response from an SmsJob entity.
     */
    public static SmsJobResponse fromEntity(SmsJob job, String message) {
        return SmsJobResponse.builder()
                .id(job.id)
                .jobType(job.jobType)
                .status(job.status)
                .approvalStatus(job.approvalStatus)
                .totalRecipients(job.totalRecipients)
                .totalSmsCount(job.totalSmsCount)
                .scheduledAt(job.scheduledAt)
                .createdAt(job.createdAt)
                .message(message)
                .build();
    }
}

package org.hmmk.sms.entity.sms;

import jakarta.persistence.*;
import lombok.*;
import org.hmmk.sms.entity.TenantScopedEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "sms_recipients", indexes = {
        @Index(name = "idx_tenant_job", columnList = "tenantId, job_id")
})
public class SmsRecipient extends TenantScopedEntity {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(nullable = false)
    public String senderId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    public SmsJob job;
    @Column(nullable = false)
    public String phoneNumber;
    public String message;

    @Column(name = "webhook_url", columnDefinition = "TEXT")
    public String webhookUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SmsJob.MessageType messageType; // ENGLISH, UNICODE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RecipientStatus status;
    public Instant sentAt;

    public enum RecipientStatus {
        PENDING, SENT, FAILED
    }
}

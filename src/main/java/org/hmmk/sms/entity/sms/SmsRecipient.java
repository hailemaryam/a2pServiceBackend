package org.hmmk.sms.entity.sms;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sms_recipients")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SmsRecipient {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(nullable = false)
    public String tenantId;

    @Column(nullable = false)
    public String senderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    public SmsJob job;

    @Column(nullable = false)
    public String phoneNumber;

    public String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SmsJob.MessageType messageType; // ENGLISH, UNICODE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RecipientStatus status;

    @CreationTimestamp
    public Instant createdAt;

    public Instant sentAt;

    public enum RecipientStatus { PENDING, SENT, FAILED }
}

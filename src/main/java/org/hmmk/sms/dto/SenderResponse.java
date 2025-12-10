package org.hmmk.sms.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hmmk.sms.entity.Sender;

import java.time.Instant;

/**
 * Response DTO for Sender entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SenderResponse {

    private String id;
    private String name;
    private String shortCode;
    private Sender.SenderStatus status;
    private String tenantId;
    private Instant createdAt;
    private Instant updatedAt;
    private String message;

    /**
     * Create a SenderResponse from a Sender entity.
     */
    public static SenderResponse fromEntity(Sender sender) {
        return fromEntity(sender, null);
    }

    /**
     * Create a SenderResponse from a Sender entity with an optional message.
     */
    public static SenderResponse fromEntity(Sender sender, String message) {
        return SenderResponse.builder()
                .id(sender.id)
                .name(sender.getName())
                .shortCode(sender.getShortCode())
                .status(sender.getStatus())
                .tenantId(sender.tenantId)
                .createdAt(sender.createdAt)
                .updatedAt(sender.updatedAt)
                .message(message)
                .build();
    }
}

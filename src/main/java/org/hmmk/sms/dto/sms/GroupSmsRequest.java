package org.hmmk.sms.dto.sms;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for sending SMS to all contacts in a contact group.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSmsRequest {

    @NotBlank(message = "Sender ID is required")
    private String senderId;

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotBlank(message = "Message content is required")
    private String message;

    /**
     * Optional scheduled time. If null, SMS will be sent immediately.
     */
    private Instant scheduledAt;
}

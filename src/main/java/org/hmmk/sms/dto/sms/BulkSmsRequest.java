package org.hmmk.sms.dto.sms;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for sending bulk SMS from CSV file upload.
 * This DTO is used alongside a multipart file upload containing phone numbers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkSmsRequest {

    @NotBlank(message = "Sender ID is required")
    private String senderId;

    @NotBlank(message = "Message content is required")
    private String message;

    /**
     * Optional scheduled time. If null, SMS will be sent immediately.
     */
    private Instant scheduledAt;
}

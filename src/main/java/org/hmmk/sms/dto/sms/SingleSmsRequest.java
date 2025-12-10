package org.hmmk.sms.dto.sms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for sending a single SMS to one phone number.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SingleSmsRequest {

    @NotBlank(message = "Sender ID is required")
    private String senderId;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Message content is required")
    private String message;

    /**
     * Optional scheduled time. If null, SMS will be sent immediately.
     */
    private Instant scheduledAt;
}

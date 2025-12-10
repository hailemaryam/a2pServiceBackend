package org.hmmk.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for creating or updating a Sender.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SenderCreateRequest {

    @NotBlank(message = "Sender name is required")
    @Size(max = 255, message = "Sender name must be at most 255 characters")
    private String name;

    @NotBlank(message = "Short code is required")
    @Size(max = 200, message = "Short code must be at most 200 characters")
    private String shortCode;
}

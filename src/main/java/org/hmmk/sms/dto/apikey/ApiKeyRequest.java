package org.hmmk.sms.dto.apikey;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyRequest {
    @NotBlank(message = "Sender ID is required")
    private String senderId;

    private String name;
}

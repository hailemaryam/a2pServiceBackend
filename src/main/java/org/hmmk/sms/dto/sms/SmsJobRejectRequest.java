package org.hmmk.sms.dto.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for rejecting an SMS job.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsJobRejectRequest {

    /**
     * Optional reason for rejection.
     */
    private String reason;
}

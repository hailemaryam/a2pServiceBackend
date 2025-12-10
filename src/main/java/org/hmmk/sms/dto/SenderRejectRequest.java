package org.hmmk.sms.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for rejecting a Sender with an optional reason.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SenderRejectRequest {

    private String reason;
}

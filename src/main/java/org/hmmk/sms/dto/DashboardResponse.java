package org.hmmk.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hmmk.sms.dto.SmsSentBySource;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    public long remainingCredits;
    public SmsSentBySource smsSentBySource;
    public long contactCount;
}

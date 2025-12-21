package org.hmmk.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {
    public long tenantCount;
    public long pendingSenderCount;
    public long pendingSmsJobCount;
    public long activePackageCount;
}


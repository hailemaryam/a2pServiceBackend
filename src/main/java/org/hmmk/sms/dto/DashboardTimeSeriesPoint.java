package org.hmmk.sms.dto;

import java.time.Instant;

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
public class DashboardTimeSeriesPoint {
    public String label;
    public Instant periodStart;
    public Instant periodEnd;
    public long totalSms;
}


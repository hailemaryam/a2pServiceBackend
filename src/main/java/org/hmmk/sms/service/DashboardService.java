package org.hmmk.sms.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.hmmk.sms.dto.DashboardOverviewResponse;
import org.hmmk.sms.dto.DashboardResponse;
import org.hmmk.sms.dto.DashboardTimeSeriesPoint;
import org.hmmk.sms.dto.SmsSentBySource;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.contact.Contact;
import org.hmmk.sms.entity.sms.SmsJob;
import org.hmmk.sms.entity.sms.SmsRecipient;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class DashboardService {

    @Inject
    EntityManager em;

    private Clock clock = Clock.systemUTC();

    void setClock(Clock clock) {
        this.clock = clock;
    }

    public DashboardResponse getDashboard(String tenantId) {
        long remaining = getRemainingCredits(tenantId);
        long contacts = getContactCount(tenantId);
        SmsSentBySource smsBySource = getTotalSmsSentBySource(tenantId);

        return DashboardResponse.builder()
                .remainingCredits(remaining)
                .contactCount(contacts)
                .smsSentBySource(smsBySource)
                .build();
    }

    public long getRemainingCredits(String tenantId) {
        Tenant t = Tenant.findById(tenantId);
        return t == null ? 0L : t.smsCredit;
    }

    public long getContactCount(String tenantId) {
        return Contact.count("tenantId", tenantId);
    }

    /**
     * Count SmsRecipient rows with status = SENT grouped by parent job.sourceType
     * Return a map that always contains the known SourceType names as keys (with 0 if none).
     */
    public SmsSentBySource getTotalSmsSentBySource(String tenantId) {
        long api = 0L;
        long manual = 0L;
        long csvUpload = 0L;

        String jpql = "SELECT j.sourceType, COUNT(r) FROM SmsRecipient r JOIN r.job j " +
                "WHERE r.tenantId = :tenantId AND r.status = :sent GROUP BY j.sourceType";

        Query q = em.createQuery(jpql);
        q.setParameter("tenantId", tenantId);
        q.setParameter("sent", org.hmmk.sms.entity.sms.SmsRecipient.RecipientStatus.SENT);

        @SuppressWarnings("unchecked")
        List<Object[]> results = q.getResultList();
        for (Object[] row : results) {
            if (row == null || row.length < 2) continue;
            Object key = row[0];
            Object val = row[1];
            if (key == null) continue;
            String k = key.toString();
            Long v = val == null ? 0L : ((Number) val).longValue();
            switch (k) {
                case "API":
                    api = v;
                    break;
                case "MANUAL":
                    manual = v;
                    break;
                case "CSV_UPLOAD":
                    csvUpload = v;
                    break;
                default:
                    // ignore unknown source types
            }
        }

        return SmsSentBySource.builder()
                .api(api)
                .manual(manual)
                .csvUpload(csvUpload)
                .build();
    }

    public DashboardOverviewResponse getSmsOverview(String tenantId, Granularity granularity) {
        int periods = 12;
        List<DashboardTimeSeriesPoint> points = buildEmptyBuckets(granularity, periods);
        Map<Instant, DashboardTimeSeriesPoint> bucketIndex = points.stream()
                .collect(Collectors.toMap(DashboardTimeSeriesPoint::getPeriodStart, Function.identity()));

        String truncExpr = "DATE_TRUNC('" + granularity.toPgUnit().toUpperCase() + "', r.sentAt)";
        String jpql = "SELECT " + truncExpr + ", COUNT(r) FROM SmsRecipient r " +
                "WHERE r.tenantId = :tenantId AND r.status = :status AND r.sentAt >= :from " +
                "GROUP BY " + truncExpr + " ORDER BY " + truncExpr;
        Query query = em.createQuery(jpql);
        query.setParameter("tenantId", tenantId);
        query.setParameter("status", SmsRecipient.RecipientStatus.SENT);
        query.setParameter("from", points.get(0).periodStart);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        for (Object[] row : results) {
            Instant bucketStart = ((Timestamp) row[0]).toInstant();
            long count = ((Number) row[1]).longValue();
            DashboardTimeSeriesPoint point = bucketIndex.get(bucketStart);
            if (point != null) {
                point.setTotalSms(count);
            }
        }

        return DashboardOverviewResponse.builder()
                .granularity(granularity.name())
                .points(points)
                .build();
    }

    private List<DashboardTimeSeriesPoint> buildEmptyBuckets(Granularity granularity, int periods) {
        List<DashboardTimeSeriesPoint> buckets = new ArrayList<>();
        Instant now = clock.instant().truncatedTo(ChronoUnit.DAYS);
        BucketAligner aligner = granularity.getAligner();
        Instant alignedEnd = aligner.alignEnd(now);
        Instant cursor = alignedEnd;
        for (int i = 0; i < periods; i++) {
            Instant start = aligner.shift(cursor, -1);
            buckets.add(DashboardTimeSeriesPoint.builder()
                    .label(aligner.label(start))
                    .periodStart(start)
                    .periodEnd(cursor)
                    .totalSms(0L)
                    .build());
            cursor = start;
        }
        Collections.reverse(buckets);
        return buckets;
    }

    public enum Granularity {
        MONTH {
            @Override
            ChronoUnit unit() { return ChronoUnit.MONTHS; }
            @Override
            String toPgUnit() { return "month"; }
        },
        QUARTER {
            @Override
            ChronoUnit unit() { return ChronoUnit.MONTHS; }
            @Override
            String toPgUnit() { return "quarter"; }
        },
        YEAR {
            @Override
            ChronoUnit unit() { return ChronoUnit.YEARS; }
            @Override
            String toPgUnit() { return "year"; }
        };

        abstract ChronoUnit unit();
        abstract String toPgUnit();

        BucketAligner getAligner() {
            switch (this) {
                case MONTH: return new MonthAligner();
                case QUARTER: return new QuarterAligner();
                case YEAR: return new YearAligner();
                default: throw new IllegalStateException("Unexpected value: " + this);
            }
        }
    }

    private interface BucketAligner {
        Instant alignEnd(Instant instant);
        Instant shift(Instant instant, int periods);
        String label(Instant instant);
    }

    private static class MonthAligner implements BucketAligner {
        @Override
        public Instant alignEnd(Instant instant) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            ZonedDateTime end = zdt.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            return end.toInstant();
        }

        @Override
        public Instant shift(Instant instant, int periods) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            return zdt.plusMonths(periods).toInstant();
        }

        @Override
        public String label(Instant instant) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            return zdt.getYear() + "-" + String.format("%02d", zdt.getMonthValue());
        }
    }

    private static class QuarterAligner implements BucketAligner {
        @Override
        public Instant alignEnd(Instant instant) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            int currentQuarter = (zdt.getMonthValue() - 1) / 3;
            ZonedDateTime end = zdt.withMonth(currentQuarter * 3 + 1)
                    .withDayOfMonth(1)
                    .truncatedTo(ChronoUnit.DAYS)
                    .plusMonths(3);
            return end.toInstant();
        }

        @Override
        public Instant shift(Instant instant, int periods) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            return zdt.plusMonths(periods * 3L).toInstant();
        }

        @Override
        public String label(Instant instant) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            int quarter = ((zdt.getMonthValue() - 1) / 3) + 1;
            return zdt.getYear() + "-Q" + quarter;
        }
    }

    private static class YearAligner implements BucketAligner {
        @Override
        public Instant alignEnd(Instant instant) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            return zdt.plusYears(1)
                    .withDayOfYear(1)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
        }

        @Override
        public Instant shift(Instant instant, int periods) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            return zdt.plusYears(periods).toInstant();
        }

        @Override
        public String label(Instant instant) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            return String.valueOf(zdt.getYear());
        }
    }
}

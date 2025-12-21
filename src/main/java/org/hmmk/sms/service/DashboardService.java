package org.hmmk.sms.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.hmmk.sms.dto.DashboardResponse;
import org.hmmk.sms.dto.SmsSentBySource;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.contact.Contact;
import org.hmmk.sms.entity.sms.SmsJob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DashboardService {

    @Inject
    EntityManager em;

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
}

package org.hmmk.sms.entity.payment;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hmmk.sms.entity.TenantScopedEntity;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sms_package_id", nullable = false)
    public SmsPackageTier smsPackage;
    @Column(nullable = false)
    public BigDecimal amountPaid; // total price paid
    @Column(name = "sms_credited", nullable = false)
    public int smsCredited; // how many SMS added to tenant balance
    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    public PaymentStatus paymentStatus;

    public enum PaymentStatus {
        SUCCESSFUL, // 0
        FAILED, // 1
        IN_PROGRESS, // 2
        CANCELED // 3
    }
}

package org.hmmk.sms.entity.payment;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "sms_package_tier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsPackageTier extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @Column(name = "min_sms_count", nullable = false)
    public int minSmsCount;

    @Column(name = "max_sms_count")
    public Integer maxSmsCount; // nullable means no upper limit

    @Column(name = "price_per_sms", nullable = false)
    public BigDecimal pricePerSms;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "is_active", nullable = false)
    public boolean isActive;
}

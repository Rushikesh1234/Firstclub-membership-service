package com.firstclub.membership.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "membership_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipPlan {
    @Id
    private String id;
    private String name;
    private String billingPeriod; // MONTHLY, QUARTERLY, YEARLY
    private BigDecimal basePrice;
    private String currency;
    private boolean isActive;
}
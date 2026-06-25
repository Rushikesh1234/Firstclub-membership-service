package com.firstclub.membership.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "membership_tiers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipTier {
    @Id
    private String id;
    @Column(unique = true)
    private Integer tierLevel; // 1=Silver, 2=Gold, 3=Platinum
    private String name;
    private Integer minOrdersRequired;
    private BigDecimal minOrderValueMonthly;
    private String targetCohortId;
}
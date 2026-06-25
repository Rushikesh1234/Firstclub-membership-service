package com.firstclub.membership.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tier_benefits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TierBenefit {

    @Id
    private String id;

    @Column(name = "tier_id", nullable = false)
    private String tierId;

    @Column(name = "benefit_type", nullable = false)
    private String benefitType; // e.g., FREE_DELIVERY, CATEGORY_DISCOUNT

    @Column(name = "configuration_json", columnDefinition = "jsonb", nullable = false)
    private String configurationJson; // Stores dynamic rules e.g. {"discount": 15}

    @Column(name = "is_configurable")
    private boolean isConfigurable;
}
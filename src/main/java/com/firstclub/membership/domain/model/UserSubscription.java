package com.firstclub.membership.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSubscription {
    @Id
    private String id;
    
    @Column(nullable = false, unique = true)
    private String userId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id")
    private MembershipPlan plan;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_tier_id")
    private MembershipTier currentTier;
    
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;
    
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private boolean isAutoRenew;
    
    @Version
    private Integer version; // Optimistic Lock handling concurrent mutations

    @Column(name = "cohort_id", nullable = false)
    private String cohortId;
}
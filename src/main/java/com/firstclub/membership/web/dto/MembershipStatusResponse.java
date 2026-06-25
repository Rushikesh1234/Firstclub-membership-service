package com.firstclub.membership.web.dto;

import com.firstclub.membership.domain.model.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MembershipStatusResponse {
    private String userId;
    private String subscriptionId;
    private String planName;
    private String tierName;
    private SubscriptionStatus status;
    private LocalDateTime expiryDate;
    private List<String> activeBenefits;
}
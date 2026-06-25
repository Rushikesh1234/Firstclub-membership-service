package com.firstclub.membership.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubscriptionRequest {
    @NotBlank(message = "User parameter constraint validation failure")
    private String userId;
    @NotBlank(message = "Plan identifier parameter constraint validation failure")
    private String planId;
    @NotBlank(message = "Tier identifier parameter constraint validation failure")
    private String tierId;
}
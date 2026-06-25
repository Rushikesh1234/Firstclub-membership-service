package com.firstclub.membership.domain.strategy;

import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.UserSubscription;

import org.springframework.stereotype.Component;

@Component
public class CohortEligibilityStrategy implements TierEligibilityStrategy {

    @Override
    public boolean isEligible(UserSubscription subscription, MembershipTier targetTier, int currentMonthOrderCount, double currentMonthSpend) {
        if (targetTier.getTargetCohortId() == null || targetTier.getTargetCohortId().isBlank()) {
            return false;
        }

        return targetTier.getTargetCohortId().equalsIgnoreCase(subscription.getCohortId());
    }

}
package com.firstclub.membership.domain.strategy;

import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.UserSubscription; // Added Import
import org.springframework.stereotype.Component;

@Component
public class OrderCountStrategy implements TierEligibilityStrategy {
    
    @Override
    public boolean isEligible(UserSubscription subscription, MembershipTier targetTier, int currentMonthOrderCount, double currentMonthSpend) {
        if (targetTier.getMinOrdersRequired() == null) {
            return false;
        }
        return currentMonthOrderCount >= targetTier.getMinOrdersRequired();
    }
}
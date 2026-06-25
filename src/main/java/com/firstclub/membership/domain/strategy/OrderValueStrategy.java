package com.firstclub.membership.domain.strategy;

import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.UserSubscription; // Added Import
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class OrderValueStrategy implements TierEligibilityStrategy {
    
    @Override
    public boolean isEligible(UserSubscription subscription, MembershipTier targetTier, int currentMonthOrderCount, double currentMonthSpend) {
        if (targetTier.getMinOrderValueMonthly() == null) {
            return false;
        }
        BigDecimal spendMapping = BigDecimal.valueOf(currentMonthSpend);
        return spendMapping.compareTo(targetTier.getMinOrderValueMonthly()) >= 0;
    }
}
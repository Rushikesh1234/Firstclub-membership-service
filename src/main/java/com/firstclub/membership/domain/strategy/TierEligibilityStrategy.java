package com.firstclub.membership.domain.strategy;

import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.UserSubscription;

public interface TierEligibilityStrategy {
    boolean isEligible(UserSubscription subscription, MembershipTier targetTier, int currentMonthOrderCount, double currentMonthSpend);
}
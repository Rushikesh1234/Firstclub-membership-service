package com.firstclub.membership.domain.strategy;

import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.UserSubscription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCountStrategyTest {

    private final OrderCountStrategy strategy = new OrderCountStrategy();

    @Test
    @DisplayName("Should qualify user when frequency of monthly order count passes required minimum counts")
    void isEligible_WhenOrderCountMeetsThreshold_ShouldReturnTrue() {
        MembershipTier tier = new MembershipTier();
        tier.setMinOrdersRequired(15);

        boolean result = strategy.isEligible(new UserSubscription(), tier, 20, 0.0);
        assertThat(result).isTrue();
    }
}
package com.firstclub.membership.domain.strategy;

import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.UserSubscription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderValueStrategyTest {

    private final OrderValueStrategy strategy = new OrderValueStrategy();

    @Test
    @DisplayName("Should pass evaluation when current monthly spend is equal or higher than tier limits")
    void isEligible_WhenSpendMeetsThreshold_ShouldReturnTrue() {
        MembershipTier tier = new MembershipTier();
        tier.setMinOrderValueMonthly(BigDecimal.valueOf(10000.00));

        boolean result = strategy.isEligible(new UserSubscription(), tier, 0, 12500.00);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should fail validation when current monthly spend slips below required value metrics")
    void isEligible_WhenSpendIsInsufficient_ShouldReturnFalse() {
        MembershipTier tier = new MembershipTier();
        tier.setMinOrderValueMonthly(BigDecimal.valueOf(10000.00));

        boolean result = strategy.isEligible(new UserSubscription(), tier, 0, 9999.00);
        assertThat(result).isFalse();
    }
}
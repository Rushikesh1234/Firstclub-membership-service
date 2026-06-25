package com.firstclub.membership.domain.strategy;

import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.UserSubscription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CohortEligibilityStrategyTest {

    private final CohortEligibilityStrategy strategy = new CohortEligibilityStrategy();

    @Test
    @DisplayName("Should return true when user subscription matches target tier cohort identifier exactly")
    void isEligible_WhenCohortMatches_ShouldReturnTrue() {
        UserSubscription sub = new UserSubscription();
        sub.setCohortId("VIP_POWER_USERS");

        MembershipTier tier = new MembershipTier();
        tier.setTargetCohortId("VIP_POWER_USERS");

        boolean result = strategy.isEligible(sub, tier, 0, 0.0);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when tier has no cohort restrictions assigned")
    void isEligible_WhenTierCohortIdNull_ShouldReturnFalse() {
        UserSubscription sub = new UserSubscription();
        sub.setCohortId("VIP_POWER_USERS");

        MembershipTier tier = new MembershipTier(); // targetCohortId is null

        boolean result = strategy.isEligible(sub, tier, 0, 0.0);
        assertThat(result).isFalse();
    }
}
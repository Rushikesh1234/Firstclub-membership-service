package com.firstclub.membership.service;

import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.SubscriptionStatus;
import com.firstclub.membership.domain.model.UserSubscription;
import com.firstclub.membership.domain.strategy.TierEligibilityStrategy;
import com.firstclub.membership.domain.repository.PlanRepository;          
import com.firstclub.membership.domain.repository.SubscriptionRepository;  
import com.firstclub.membership.domain.repository.TierRepository;    
import com.firstclub.membership.web.dto.SubscriptionRequest;


import org.springframework.data.redis.core.RedisTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null") // Suppresses strict IDE null type-safety analysis warnings across all Mockito matchers
class MembershipServiceImplTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PlanRepository planRepository;
    @Mock private TierRepository tierRepository;
    @Mock private RedisTemplate<String, Object> redisTemplate;

    @Spy 
    private List<TierEligibilityStrategy> eligibilityStrategies = new ArrayList<>();

    @Mock private TierEligibilityStrategy mockStrategy;

    @InjectMocks
    private MembershipServiceImpl membershipService;

    private UserSubscription activeSubscription;
    private UserSubscription cancelledSubscription;
    private MembershipTier silverTier;
    private MembershipTier goldTier;

    @BeforeEach
    void setUp() {
        silverTier = new MembershipTier();
        silverTier.setId("tier_silver");
        silverTier.setTierLevel(1);
        silverTier.setName("Silver");

        goldTier = new MembershipTier();
        goldTier.setId("tier_gold");
        goldTier.setTierLevel(2);
        goldTier.setName("Gold");

        activeSubscription = new UserSubscription();
        activeSubscription.setUserId("user_dev_03");
        activeSubscription.setStatus(SubscriptionStatus.ACTIVE);
        activeSubscription.setCurrentTier(silverTier);
        activeSubscription.setCohortId("STANDARD_COHORT");

        cancelledSubscription = new UserSubscription();
        cancelledSubscription.setUserId("user_dev_01");
        cancelledSubscription.setStatus(SubscriptionStatus.CANCELLED);
        cancelledSubscription.setCurrentTier(silverTier);
    }

    @Test
    @DisplayName("Should gracefully throw IllegalArgumentException when attempting to modify a CANCELLED subscription")
    void changeTierOrPlan_WhenCancelled_ShouldThrowException() {
        SubscriptionRequest request = new SubscriptionRequest();
        request.setUserId("user_dev_01");
        request.setPlanId("plan_monthly");
        request.setTierId("tier_gold");

        when(subscriptionRepository.findByUserId("user_dev_01")).thenReturn(Optional.of(cancelledSubscription));

        assertThatThrownBy(() -> membershipService.changeTierOrPlan("user_dev_01", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot modify or upgrade a cancelled subscription profile");

        verify(subscriptionRepository, never()).save(isA(UserSubscription.class));
    }

    @Test
    @DisplayName("Should short circuit reevaluation loop immediately if user is CANCELLED")
    void processTierReevaluation_WhenCancelled_ShouldBypassStrategies() {
        when(subscriptionRepository.findByUserId("user_dev_01")).thenReturn(Optional.of(cancelledSubscription));

        membershipService.processTierReevaluation("user_dev_01", 50, 50000.0);

        verify(tierRepository, never()).findAllByOrderByTierLevelDesc();
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully promote user tier when dynamic strategy qualifies eligibility rules")
    void processTierReevaluation_WhenQualifies_ShouldUpgradeTier() {
        eligibilityStrategies.add(mockStrategy);
        
        when(subscriptionRepository.findByUserId("user_dev_03")).thenReturn(Optional.of(activeSubscription));
        when(tierRepository.findAllByOrderByTierLevelDesc()).thenReturn(List.of(goldTier, silverTier));
        when(mockStrategy.isEligible(eq(activeSubscription), eq(goldTier), eq(45), eq(35000.0))).thenReturn(true);
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        membershipService.processTierReevaluation("user_dev_03", 45, 35000.0);

        assertThat(activeSubscription.getCurrentTier().getId()).isEqualTo("tier_gold");
        assertThat(activeSubscription.getStatus()).isEqualTo(SubscriptionStatus.UPGRADED);
        verify(subscriptionRepository, times(1)).save(activeSubscription);
    }
}
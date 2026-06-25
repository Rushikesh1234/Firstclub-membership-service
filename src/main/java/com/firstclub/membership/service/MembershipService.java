package com.firstclub.membership.service;

import com.firstclub.membership.domain.model.UserSubscription;
import com.firstclub.membership.web.dto.MembershipStatusResponse;
import com.firstclub.membership.web.dto.SubscriptionRequest;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.MembershipTier;
import java.util.*;

public interface MembershipService {
    UserSubscription subscribeUser(SubscriptionRequest request);
    UserSubscription changeTierOrPlan(String userId, SubscriptionRequest request);
    void cancelSubscription(String userId);
    Optional<MembershipStatusResponse> getMembershipStatus(String userId);
    void processTierReevaluation(String userId, int currentMonthOrderCount, double currentMonthSpend);
    List<MembershipPlan> getCatalogPlans();
    List<MembershipTier> getCatalogTiers();
}
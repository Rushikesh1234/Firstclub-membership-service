package com.firstclub.membership.service;

import com.firstclub.membership.domain.model.*;
import com.firstclub.membership.domain.repository.*;
import com.firstclub.membership.domain.strategy.TierEligibilityStrategy;
import com.firstclub.membership.exception.ResourceNotFoundException;
import com.firstclub.membership.web.dto.MembershipStatusResponse;
import com.firstclub.membership.web.dto.SubscriptionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class MembershipServiceImpl implements MembershipService {

    private final SubscriptionRepository subscriptionRepository;
    private final TierRepository tierRepository;
    private final PlanRepository planRepository;
    private final TierBenefitRepository tierBenefitRepository;
    private final List<TierEligibilityStrategy> eligibilityStrategies;
    
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CACHE_PREFIX = "user:membership:";
    private static final String LOCK_PREFIX = "lock:membership:modify:";

    @Override
    @Transactional
    public UserSubscription subscribeUser(SubscriptionRequest request) {
        String lockKey = LOCK_PREFIX + request.getUserId();
        RLock distributedLock = redissonClient.getLock(lockKey);
        
        try {
            if (!distributedLock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Operation already processing for user. Please avoid concurrent requests.");
            }
            
            MembershipPlan selectedPlan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requested Membership Plan not found"));
            MembershipTier selectedTier = tierRepository.findById(request.getTierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requested Membership Tier not found"));

            Optional<UserSubscription> existingSubOpt = subscriptionRepository.findByUserId(request.getUserId());
            UserSubscription targetSubscription;

            if (existingSubOpt.isPresent()) {
                UserSubscription existingSub = existingSubOpt.get();
                
                if (existingSub.getStatus() == SubscriptionStatus.ACTIVE || 
                    existingSub.getStatus() == SubscriptionStatus.UPGRADED || 
                    existingSub.getStatus() == SubscriptionStatus.DOWNGRADED) {
                    throw new IllegalArgumentException("User already has an active membership profile.");
                }
                
                existingSub.setPlan(selectedPlan);
                existingSub.setCurrentTier(selectedTier);
                existingSub.setStatus(SubscriptionStatus.ACTIVE);
                existingSub.setStartDate(LocalDateTime.now());
                existingSub.setExpiryDate(calculateExpiry(selectedPlan.getBillingPeriod()));
                existingSub.setAutoRenew(true);
                
                targetSubscription = existingSub;
                log.info("Reactivating existing cancelled subscription profile for user: {}", request.getUserId());
            } else {
                targetSubscription = UserSubscription.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(request.getUserId())
                        .plan(selectedPlan)
                        .currentTier(selectedTier)
                        .status(SubscriptionStatus.ACTIVE)
                        .startDate(LocalDateTime.now())
                        .expiryDate(calculateExpiry(selectedPlan.getBillingPeriod()))
                        .isAutoRenew(true)
                        .build();
                log.info("Provisioning brand new subscription record for user: {}", request.getUserId());
            }

            UserSubscription saved = subscriptionRepository.save(targetSubscription);
            clearUserCache(request.getUserId());
            return saved;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread execution interrupted during locking phase.", e);
        } finally {
            if (distributedLock.isHeldByCurrentThread()) {
                distributedLock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public UserSubscription changeTierOrPlan(String userId, SubscriptionRequest request) {
        
        UserSubscription userSubscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Active subscription metadata not found for user"));
        
        
        if (userSubscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot modify or upgrade a cancelled subscription profile. Please use the subscribe endpoint to reactivate your plan.");
        }
        
        MembershipPlan targetPlan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Target billing plan not found"));
        MembershipTier targetTier = tierRepository.findById(request.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("Target membership tier not found"));

        if (targetTier.getTierLevel() > userSubscription.getCurrentTier().getTierLevel()) {
            userSubscription.setStatus(SubscriptionStatus.UPGRADED);
        } else if (targetTier.getTierLevel() < userSubscription.getCurrentTier().getTierLevel()) {
            userSubscription.setStatus(SubscriptionStatus.DOWNGRADED);
        }

        userSubscription.setCurrentTier(targetTier);
        userSubscription.setPlan(targetPlan);
        userSubscription.setExpiryDate(calculateExpiry(targetPlan.getBillingPeriod()));
        
        UserSubscription updated = subscriptionRepository.save(userSubscription);
        clearUserCache(userId);
        return updated;
    }

    @Override
    @Transactional
    public void cancelSubscription(String userId) {
        UserSubscription userSubscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Active membership record not found"));
        userSubscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(userSubscription);
        clearUserCache(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MembershipStatusResponse> getMembershipStatus(String userId) {
        String cacheKey = CACHE_PREFIX + userId;
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        UserSubscription sub;
        
        if (cachedObj != null) {
            log.info("Cache hit verified for user metadata: {}", userId);
            sub = (UserSubscription) cachedObj;
        } else {
            log.warn("Cache miss. Sourcing dataset from physical database tables.");
            Optional<UserSubscription> dbResponse = subscriptionRepository.findByUserId(userId);
            if (dbResponse.isEmpty()) {
                return Optional.empty();
            }
            sub = dbResponse.get();
            redisTemplate.opsForValue().set(cacheKey, sub, 12, TimeUnit.HOURS);
        }
        
        Objects.requireNonNull(sub, "Subscription record context must not resolve to null");

        List<String> resolvedBenefits = tierBenefitRepository.findByTierId(sub.getCurrentTier().getId())
                .stream()
                .map(b -> b.getBenefitType() + " -> " + b.getConfigurationJson())
                .toList();

        return Optional.of(MembershipStatusResponse.builder()
                .userId(sub.getUserId())
                .subscriptionId(sub.getId())
                .planName(sub.getPlan().getName())
                .tierName(sub.getCurrentTier().getName())
                .status(sub.getStatus())
                .expiryDate(sub.getExpiryDate())
                .activeBenefits(resolvedBenefits)
                .build());
    }

    @Override
    @Transactional
    public void processTierReevaluation(String userId, int currentMonthOrderCount, double currentMonthSpend) {
        log.info("Processing milestone rule matrices targeting user: {}", userId);
        
        Optional<UserSubscription> optionalSub = subscriptionRepository.findByUserId(userId);
        if (optionalSub.isEmpty()) return;
        UserSubscription subscription = optionalSub.get();
        
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            log.info("Skipping tier reevaluation for user {} - Subscription is CANCELLED.", userId);
            return;
        }
            
        List<MembershipTier> allTiers = tierRepository.findAllByOrderByTierLevelDesc();
        
        for (MembershipTier tier : allTiers) {
            if (tier.getTierLevel() <= subscription.getCurrentTier().getTierLevel()) {
                continue; 
            }
            
            // Pass the entire database record context directly into the strategies
            boolean qualifies = eligibilityStrategies.stream()
                    .anyMatch(strategy -> strategy.isEligible(subscription, tier, currentMonthOrderCount, currentMonthSpend));
                    
            if (qualifies) {
                log.info("User {} passed criteria. Advancing to tier: {}", userId, tier.getName());
                subscription.setCurrentTier(tier);
                subscription.setStatus(SubscriptionStatus.UPGRADED);
                subscriptionRepository.save(subscription);
                clearUserCache(userId);
                break;
            }
        }
    }

    @Override
    public List<MembershipPlan> getCatalogPlans() {
        return planRepository.findByIsActiveTrue();
    }

    @Override
    public List<MembershipTier> getCatalogTiers() {
        return tierRepository.findAllByOrderByTierLevelDesc();
    }

    private LocalDateTime calculateExpiry(String billingPeriod) {
        return switch (billingPeriod.toUpperCase()) {
            case "QUARTERLY" -> LocalDateTime.now().plusMonths(3);
            case "YEARLY" -> LocalDateTime.now().plusYears(1);
            default -> LocalDateTime.now().plusMonths(1);
        };
    }

    private void clearUserCache(String userId) {
        redisTemplate.delete(CACHE_PREFIX + userId);
    }
}
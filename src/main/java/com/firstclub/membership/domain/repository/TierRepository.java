package com.firstclub.membership.domain.repository;

import com.firstclub.membership.domain.model.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;

public interface TierRepository extends JpaRepository<MembershipTier, String> {
    @Cacheable(value = "tiers-config")
    List<MembershipTier> findAllByOrderByTierLevelDesc();
}
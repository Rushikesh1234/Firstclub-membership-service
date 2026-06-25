package com.firstclub.membership.domain.repository;

import com.firstclub.membership.domain.model.MembershipPlan;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlanRepository extends JpaRepository<MembershipPlan, String> {
    @Cacheable(value = "active-plans")
    List<MembershipPlan> findByIsActiveTrue();
}
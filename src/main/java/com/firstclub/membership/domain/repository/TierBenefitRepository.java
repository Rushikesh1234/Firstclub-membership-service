package com.firstclub.membership.domain.repository;

import com.firstclub.membership.domain.model.TierBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TierBenefitRepository extends JpaRepository<TierBenefit, String> {
    List<TierBenefit> findByTierId(String tierId);
}
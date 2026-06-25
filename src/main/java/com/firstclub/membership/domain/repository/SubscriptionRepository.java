package com.firstclub.membership.domain.repository;

import com.firstclub.membership.domain.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<UserSubscription, String> {
   
    @Query("SELECT s FROM UserSubscription s " +
           "JOIN FETCH s.plan " +
           "JOIN FETCH s.currentTier " +
           "WHERE s.userId = :userId")
    Optional<UserSubscription> findByUserId(@Param("userId") String userId);
}
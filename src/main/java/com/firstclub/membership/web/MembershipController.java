package com.firstclub.membership.web;

import com.firstclub.membership.domain.model.UserSubscription;
import com.firstclub.membership.service.MembershipService;
import com.firstclub.membership.web.dto.MembershipStatusResponse;
import com.firstclub.membership.web.dto.SubscriptionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @PostMapping("/subscribe")
    public ResponseEntity<UserSubscription> createSubscription(@Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(membershipService.subscribeUser(request));
    }

    @PutMapping("/modify/{userId}")
    public ResponseEntity<UserSubscription> updateSubscription(@PathVariable String userId, @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(membershipService.changeTierOrPlan(userId, request));
    }

    @DeleteMapping("/cancel/{userId}")
    public ResponseEntity<Void> cancelSubscription(@PathVariable String userId) {
        membershipService.cancelSubscription(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<MembershipStatusResponse> checkStatus(@PathVariable String userId) {
        return membershipService.getMembershipStatus(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/catalog")
    public ResponseEntity<java.util.Map<String, Object>> getCatalogOptions() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("plans", membershipService.getCatalogPlans());
        response.put("tiers", membershipService.getCatalogTiers());
        return ResponseEntity.ok(response);
    }
}
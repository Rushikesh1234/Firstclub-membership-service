package com.firstclub.membership.messaging.consumer;

import com.firstclub.membership.messaging.dto.OrderEvent;
import com.firstclub.membership.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final MembershipService membershipService;

    @KafkaListener(
            topics = "order-milestone-events",
            groupId = "membership-evaluation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMilestoneEvent(OrderEvent event) {
        log.info("Received Kafka milestone event for verification. User: {}, Orders: {}, Spend: {}", 
                event.getUserId(), event.getCurrentMonthOrderCount(), event.getCurrentMonthSpend());
        
        try {
            membershipService.processTierReevaluation(
                    event.getUserId(), 
                    event.getCurrentMonthOrderCount(), 
                    event.getCurrentMonthSpend()
            );
        } catch (Exception e) {
            // For Dead Letter Queue Handling
            log.error("Failed to process tier reevaluation for user: {}", event.getUserId(), e);
        }
    }
}
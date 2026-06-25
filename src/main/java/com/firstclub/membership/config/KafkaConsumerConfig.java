package com.firstclub.membership.config;

import com.firstclub.membership.service.MembershipService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerConfig {

    private final MembershipService membershipService;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "membership-tier-evaluators");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Read raw Strings out of Kafka first to keep the engine polymorphic
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
    }

    @Bean
    @SuppressWarnings("null")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // This converter inspects the @KafkaListener method arguments and casts JSON dynamically
        factory.setRecordMessageConverter(jsonMessageConverter());
        return factory;
    }

    @Bean
    public RecordMessageConverter jsonMessageConverter() {
        return new StringJsonMessageConverter();
    }

    @KafkaListener(topics = "order-delivered-events", groupId = "membership-tier-evaluators")
    public void consumeOrderDeliveredEvent(OrderDeliveredEvent event) {
        log.info("Received asynchronous order payload from order-delivered-events: {}", event);
        try {
            membershipService.processTierReevaluation(
                    event.getUserId(), 
                    event.getCurrentMonthOrderCount(), 
                    event.getCurrentMonthSpend()
            );
        } catch (Exception e) {
            log.error("Critical failure executing out-of-band tier calculations for user: {}", event.getUserId(), e);
        }
    }

    @Data
    public static class OrderDeliveredEvent {
        private String orderId;
        private String userId;
        private int currentMonthOrderCount;
        private double currentMonthSpend;
    }
}
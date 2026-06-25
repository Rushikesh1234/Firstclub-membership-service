package com.firstclub.membership.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String userId;
    private int currentMonthOrderCount;
    private double currentMonthSpend;
}
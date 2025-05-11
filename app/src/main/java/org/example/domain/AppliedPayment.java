package org.example.domain;

import java.math.BigDecimal;
import java.util.Map;

public record AppliedPayment(
        String orderId,
        BigDecimal finalPricePaid,
        PaymentStrategyType strategyType,
        Map<PaymentMethod, BigDecimal> actualAmountSpent
) {}

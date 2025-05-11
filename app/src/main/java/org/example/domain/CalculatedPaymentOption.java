package org.example.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public record CalculatedPaymentOption(Order order,
                                      BigDecimal finalPrice,
                                      BigDecimal discountAmount,
                                      PaymentStrategyType paymentStrategyType,
                                      Map<PaymentMethod, BigDecimal> spent
) {
    public CalculatedPaymentOption {
        discountAmount = order.value().subtract(finalPrice).setScale(2, RoundingMode.HALF_UP);
    }
}

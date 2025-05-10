package org.example.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record Order(String id, BigDecimal value, List<PaymentMethod> applicablePromotions) {
    public Order{
        value = value.setScale(2, RoundingMode.HALF_UP);
    }
}

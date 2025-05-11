package org.example.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record Order(String id, BigDecimal value, List<PaymentMethod> applicablePromotions) {
    public Order{
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Order value cannot be a negative number " + value);
        }
        if(value.scale() > 2) {
            throw new IllegalArgumentException("Order value cannot have more than 2 decimal places: " + value);
        }

        value = value.setScale(2, RoundingMode.HALF_UP);
    }
}

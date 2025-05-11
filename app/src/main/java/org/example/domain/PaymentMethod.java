package org.example.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class PaymentMethod {
    private final String id;
    private final BigDecimal discountPercentage;
    private final BigDecimal methodLimit;
    @Setter
    private BigDecimal currentLimit;

    public PaymentMethod(String id, BigDecimal discountPercentage, BigDecimal methodLimit) {
        if(methodLimit.scale() > 2) {
            throw new IllegalArgumentException("Method limit cannot have more than 2 decimal places: " + methodLimit);
        }
        this.id = id;
        this.discountPercentage = discountPercentage;
        this.methodLimit = methodLimit.setScale(2, RoundingMode.HALF_UP);
        this.currentLimit = methodLimit;
    }

    public boolean isPoints() {
        return "PUNKTY".equals(id);
    }
}

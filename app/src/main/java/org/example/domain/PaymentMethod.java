package org.example.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentMethod {
    private final String id;
    private final BigDecimal discountPercentage;
    private final BigDecimal methodLimit;
    private BigDecimal currentLimit;

    public PaymentMethod(String id, BigDecimal discountPercentage, BigDecimal methodLimit) {
        this.id = id;
        this.discountPercentage = discountPercentage;
        this.methodLimit = methodLimit.setScale(2, RoundingMode.HALF_UP);
        this.currentLimit = methodLimit;
    }
}

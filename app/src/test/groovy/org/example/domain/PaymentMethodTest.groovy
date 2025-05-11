package org.example.domain

import spock.lang.Specification
import spock.lang.Unroll

import java.math.BigDecimal
import java.math.RoundingMode;


class PaymentMethodTest extends Specification {
    def "should correctly initialize PaymentMethod with constructor"() {
        given:
        String id = "CreditCard"
        BigDecimal discountPercentage = new BigDecimal("0.05")
        BigDecimal methodLimitParam = new BigDecimal("1000.13")

        when:
        PaymentMethod paymentMethod = new PaymentMethod(id, discountPercentage, methodLimitParam)

        then:
        paymentMethod.getId() == id
        paymentMethod.getDiscountPercentage() == discountPercentage
        paymentMethod.getMethodLimit() == methodLimitParam
        paymentMethod.getMethodLimit().scale() == 2
    }

    def "should allow setting and getting currentLimit"() {
        given:
        PaymentMethod paymentMethod = new PaymentMethod("TEST_ID", BigDecimal.ZERO, new BigDecimal("100.00"))
        BigDecimal newLimit = new BigDecimal("50.55")

        when:
        paymentMethod.setCurrentLimit(newLimit)

        then:
        paymentMethod.getCurrentLimit() == newLimit
        paymentMethod.getCurrentLimit().scale() == 2
    }

    def "should not initialize PaymentMethod with 3 decimal limit"() {
        given:
        String id = "TestCardScale"
        BigDecimal discount = new BigDecimal("10")
        BigDecimal limitWithThreeDecimals = new BigDecimal("100.123")

        when:
        new PaymentMethod(id, discount, limitWithThreeDecimals)

        then:
        def exception = thrown(IllegalArgumentException)

        exception.message == String.format("Method limit cannot have more than 2 decimal places: %s", limitWithThreeDecimals)
    }
}

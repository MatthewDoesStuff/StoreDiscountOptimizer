package org.example.domain

import spock.lang.Specification

class OrderTest extends Specification {
    def "should correctly initialize Order with id and promotions, and transformed value"() {
        given:
        String orderId = "ORD123"
        BigDecimal value = new BigDecimal("123.45")
        List<PaymentMethod> promotions = [
                new PaymentMethod("PROMO1", BigDecimal.ZERO, BigDecimal.TEN),
                new PaymentMethod("PROMO2", BigDecimal.ONE, BigDecimal.ONE)
        ]

        when:
        Order order = new Order(orderId, value, promotions)

        then:
        order.id() == orderId
        order.value() == value
        order.value().scale() == 2
        order.applicablePromotions() == promotions
        order.applicablePromotions().is(promotions)
    }

    def "should correctly initialize with an empty list of promotions and value with one decimal"() {
        given:
        String orderId = "ORD_EMPTY_PROMO"
        BigDecimal initialValue = new BigDecimal("75.7")
        BigDecimal expectedTransformedValue = new BigDecimal("75.70")
        List<PaymentMethod> emptyPromotions = Collections.emptyList()

        when:
        Order order = new Order(orderId, initialValue, emptyPromotions)

        then:
        order.id() == orderId
        order.value() == expectedTransformedValue
        order.applicablePromotions() == emptyPromotions
        order.applicablePromotions().isEmpty()
    }

    def "should not initialize Order with 3 decimal value"() {
        given:
        String orderId = "ORD_INVALID"
        BigDecimal invalidValue = new BigDecimal("123.456")
        List<PaymentMethod> promotions = Collections.emptyList()

        when:
        new Order(orderId, invalidValue, promotions)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == String.format("Order value cannot have more than 2 decimal places: %s", invalidValue)
    }
}

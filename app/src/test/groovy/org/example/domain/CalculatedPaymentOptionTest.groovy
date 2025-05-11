package org.example.domain

import spock.lang.Shared
import spock.lang.Specification

class CalculatedPaymentOptionTest extends Specification {
    @Shared
    PaymentMethod sampleCard = new PaymentMethod("CARD_A", BigDecimal.ZERO, new BigDecimal("1000"))
    @Shared
    Order sampleOrder = new Order("ORDER_SAMPLE", new BigDecimal("100.00"), [sampleCard])


    def "constructor should initialize CalculatedPaymentOption correctly with valid arguments"() {
        given:
        BigDecimal finalPrice = new BigDecimal("90.00")
        BigDecimal discountAmount = new BigDecimal("10.00")
        PaymentStrategyType strategy = PaymentStrategyType.FULL_CARD_WITH_PROMOTION
        Map<PaymentMethod, BigDecimal> spentAmounts = Collections.singletonMap(sampleCard, new BigDecimal("90.00"))

        when:
        CalculatedPaymentOption option = new CalculatedPaymentOption(sampleOrder, finalPrice, discountAmount, strategy, spentAmounts)

        then:
        option.order() == sampleOrder
        option.finalPrice() == finalPrice
        option.discountAmount() == discountAmount
        option.paymentStrategyType() == strategy
        option.spent() == spentAmounts
        option.spent().size() == 1
        option.spent().get(sampleCard) == new BigDecimal("90.00")
    }

    def "constructor should handle empty spentAmounts map for zero final price"() {
        given:
        Order zeroValueOrder = new Order("ORDER_ZERO", BigDecimal.ZERO, [])
        BigDecimal finalPrice = BigDecimal.ZERO
        BigDecimal discountAmount = BigDecimal.ZERO
        PaymentStrategyType strategy = PaymentStrategyType.FULL_PAYMENT_NO_PROMOTION
        Map<PaymentMethod, BigDecimal> emptySpentAmounts = Collections.emptyMap()

        when:
        CalculatedPaymentOption option = new CalculatedPaymentOption(zeroValueOrder, finalPrice, discountAmount, strategy, emptySpentAmounts)

        then:
        option.order() == zeroValueOrder
        option.finalPrice() == BigDecimal.ZERO
        option.discountAmount() == BigDecimal.ZERO
        option.paymentStrategyType() == strategy
        option.spent().isEmpty()
    }
}

package org.example.domain

import spock.lang.Shared
import spock.lang.Specification

class AppliedPaymentTest extends Specification {
    @Shared
    PaymentMethod sampleCard = new PaymentMethod("VISA_CARD", BigDecimal.ZERO, new BigDecimal("1000"))

    def "constructor should initialize AppliedPayment correctly with valid arguments"() {
        given:
        String orderId = "ORDER_XYZ"
        BigDecimal finalPrice = new BigDecimal("95.50")
        PaymentStrategyType strategy = PaymentStrategyType.FULL_CARD_WITH_PROMOTION
        Map<PaymentMethod, BigDecimal> spentAmounts = Collections.singletonMap(sampleCard, new BigDecimal("95.50"))

        when:
        AppliedPayment payment = new AppliedPayment(orderId, finalPrice, strategy, spentAmounts)

        then:
        payment.orderId() == orderId
        payment.finalPricePaid() == finalPrice
        payment.strategyType() == strategy
        payment.actualAmountSpent() == spentAmounts
        payment.actualAmountSpent().size() == 1
        payment.actualAmountSpent().get(sampleCard) == new BigDecimal("95.50")
    }

    def "constructor should handle empty actualAmountsSpent map"() {
        given:
        String orderId = "ORDER_ZERO_VALUE"
        BigDecimal finalPrice = BigDecimal.ZERO
        PaymentStrategyType strategy = PaymentStrategyType.FULL_PAYMENT_NO_PROMOTION
        Map<PaymentMethod, BigDecimal> emptySpentAmounts = Collections.emptyMap()

        when:
        AppliedPayment payment = new AppliedPayment(orderId, finalPrice, strategy, emptySpentAmounts)

        then:
        payment.orderId() == orderId
        payment.finalPricePaid() == BigDecimal.ZERO
        payment.strategyType() == strategy
        payment.actualAmountSpent().isEmpty()
    }
}

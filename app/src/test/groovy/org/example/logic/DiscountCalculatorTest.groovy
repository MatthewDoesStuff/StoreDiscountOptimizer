package org.example.logic

import org.example.domain.CalculatedPaymentOption
import org.example.domain.Order
import org.example.domain.PaymentMethod
import org.example.domain.PaymentStrategyType
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import java.math.RoundingMode


class DiscountCalculatorTest extends Specification {
    @Shared
    PaymentMethod pointsMethod_15_off
    @Shared
    PaymentMethod pointsMethod_0_off
    @Shared
    PaymentMethod card_mZysk_10_off
    @Shared
    PaymentMethod card_BosBankrut_5_off
    @Shared
    PaymentMethod card_Visa_0_off

    def setupSpec() {
        pointsMethod_15_off = new PaymentMethod("PUNKTY", new BigDecimal("15"), new BigDecimal("1000.00"))
        pointsMethod_0_off = new PaymentMethod("PUNKTY", BigDecimal.ZERO, new BigDecimal("1000.00"))
        card_mZysk_10_off = new PaymentMethod("mZysk", new BigDecimal("10"), new BigDecimal("1000.00"))
        card_BosBankrut_5_off = new PaymentMethod("BosBankrut", new BigDecimal("5"), new BigDecimal("1000.00"))
        card_Visa_0_off = new PaymentMethod("Visa", BigDecimal.ZERO, new BigDecimal("1000.00"))
    }

    def "S1: should generate option for an applicable card promotion"() {
        given:
        def order = new Order("O1", new BigDecimal("100.00"), [card_mZysk_10_off])
        def calculator = new DiscountCalculator(pointsMethod_15_off, [card_mZysk_10_off, card_Visa_0_off])

        when:
        List<CalculatedPaymentOption> options = calculator.calculateOptionsForOrder(order)

        then:
        def s1Option = options.find { it.paymentStrategyType() == PaymentStrategyType.FULL_CARD_WITH_PROMOTION && it.spent().containsKey(card_mZysk_10_off) }
        s1Option != null
        s1Option.finalPrice() == new BigDecimal("90.00")
        s1Option.discountAmount() == new BigDecimal("10.00")
        s1Option.spent().get(card_mZysk_10_off) == new BigDecimal("90.00")
    }

    def "S1: should generate multiple S1 options if multiple card promotions are applicable"() {
        given:
        def order = new Order("O_MULTI", new BigDecimal("200.00"), [card_mZysk_10_off, card_BosBankrut_5_off])
        def calculator = new DiscountCalculator(pointsMethod_15_off, [card_mZysk_10_off, card_BosBankrut_5_off])

        when:
        List<CalculatedPaymentOption> options = calculator.calculateOptionsForOrder(order)

        then:
        def mZyskOption = options.find { it.paymentStrategyType() == PaymentStrategyType.FULL_CARD_WITH_PROMOTION && it.spent().containsKey(card_mZysk_10_off) }
        mZyskOption != null
        mZyskOption.finalPrice() == new BigDecimal("180.00")

        def bosBankrutOption = options.find { it.paymentStrategyType() == PaymentStrategyType.FULL_CARD_WITH_PROMOTION && it.spent().containsKey(card_BosBankrut_5_off) }
        bosBankrutOption != null
        bosBankrutOption.finalPrice() == new BigDecimal("190.00")
    }

    def "S2: should generate full points payment option with PUNKTY specific discount"() {
        given:
        def order = new Order("O_POINTS_FULL", new BigDecimal("100.00"), [])
        def calculator = new DiscountCalculator(pointsMethod_15_off, [card_Visa_0_off])

        when:
        List<CalculatedPaymentOption> options = calculator.calculateOptionsForOrder(order)

        then:
        def s2Option = options.find { it.paymentStrategyType() == PaymentStrategyType.FULL_POINTS_OWN_DISCOUNT }
        s2Option != null
        s2Option.finalPrice() == new BigDecimal("85.00")
        s2Option.spent().get(pointsMethod_15_off) == new BigDecimal("85.00")
    }

    def "S2: should not generate S2 option if no points method provided to calculator"() {
        given:
        def order = new Order("O_POINTS_FULL", new BigDecimal("100.00"), [])
        def calculator = new DiscountCalculator(null, [card_Visa_0_off])

        when:
        List<CalculatedPaymentOption> options = calculator.calculateOptionsForOrder(order)

        then:
        options.every { it.paymentStrategyType() != PaymentStrategyType.FULL_POINTS_OWN_DISCOUNT }
    }

    def "S3: should not generate S3 option if points cannot cover min 10 percent or final price too low"() {
        given:
        def orderTiny = new Order("OTINY", new BigDecimal("0.01"), [])
        def calculator = new DiscountCalculator(pointsMethod_15_off, [card_Visa_0_off])

        when:
        List<CalculatedPaymentOption> options = calculator.calculateOptionsForOrder(orderTiny)

        then:
        options.every { it.paymentStrategyType() != PaymentStrategyType.PARTIAL_POINTS_10_PERCENT_GLOBAL_DISCOUNT }
    }

    def "S3: should not generate S3 if card payment would be zero or less (delegated to S2)"() {
        given:
        def orderVerySmall = new Order("OVSML", new BigDecimal("0.01"), [])
        def calculator = new DiscountCalculator(pointsMethod_15_off, [card_Visa_0_off])

        when:
        List<CalculatedPaymentOption> options = calculator.calculateOptionsForOrder(orderVerySmall)
        System.out.println(options)

        then:
        options.every { it.paymentStrategyType() != PaymentStrategyType.PARTIAL_POINTS_10_PERCENT_GLOBAL_DISCOUNT }
    }

    def "S4: should generate full payment no promotion for each client card"() {
        given:
        def order = new Order("O_NO_PROMO", new BigDecimal("77.00"), [])
        def calculator = new DiscountCalculator(pointsMethod_15_off, [pointsMethod_15_off, card_mZysk_10_off, card_BosBankrut_5_off, card_Visa_0_off])


        when:
        List<CalculatedPaymentOption> options = calculator.calculateOptionsForOrder(order)

        then:
        def s4Options = options.findAll { it.paymentStrategyType() == PaymentStrategyType.FULL_PAYMENT_NO_PROMOTION }
        s4Options.size() == 3

        def visaOption = s4Options.find { it.spent().containsKey(card_Visa_0_off) }
        visaOption != null
        visaOption.finalPrice() == new BigDecimal("77.00")
        visaOption.discountAmount() == BigDecimal.ZERO
    }

    def "should return empty list if calculator has no payment methods at all"() {
        given:
        def order = new Order("O_NOPAY", new BigDecimal("100.00"), [])
        def calculator = new DiscountCalculator(null, [])

        when:
        List<CalculatedPaymentOption> options = calculator.calculateOptionsForOrder(order)

        then:
        options.isEmpty()
    }

    def "distinct() should remove identical options if any (though unlikely with current logic)"() {
        given:

        def order = new Order("O_DISTINCT", new BigDecimal("100.00"), [card_Visa_0_off])
        def calculator = new DiscountCalculator(null, [card_Visa_0_off])

        when:
        List<CalculatedPaymentOption> options = calculator.calculateOptionsForOrder(order)

        then:
        options.size() == 2
        options.count { it.paymentStrategyType() == PaymentStrategyType.FULL_CARD_WITH_PROMOTION } == 1
        options.count { it.paymentStrategyType() == PaymentStrategyType.FULL_PAYMENT_NO_PROMOTION } == 1
    }
}

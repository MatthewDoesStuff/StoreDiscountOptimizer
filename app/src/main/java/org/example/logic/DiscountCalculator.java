package org.example.logic;

import org.example.domain.CalculatedPaymentOption;
import org.example.domain.Order;
import org.example.domain.PaymentMethod;
import org.example.domain.PaymentStrategyType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiscountCalculator {
    private final PaymentMethod pointsPaymentMethod;
    private final List<PaymentMethod> clientCardPaymentMethods;

    public DiscountCalculator(PaymentMethod pointsPaymentMethod, List<PaymentMethod> allClientCardPaymentMethods) {
        this.pointsPaymentMethod = pointsPaymentMethod;
        this.clientCardPaymentMethods = allClientCardPaymentMethods.stream()
                                            .filter(paymentMethod -> paymentMethod != null && !paymentMethod.isPoints())
                                            .collect(Collectors.toList());
    }

    public List<CalculatedPaymentOption> calculateOptionsForOrder(Order order) {
        List<CalculatedPaymentOption> options = new ArrayList<>();
        BigDecimal originalValue = order.value();

        cardWithPromotion(options, order, originalValue);
        fullPointsPayment(options, order, originalValue);
        minTenPctPointsPayment(options, order, originalValue);
        fullCardPaymentNoPromotion(options, order, originalValue);

        return options.stream().distinct().collect(Collectors.toList());
    }

    private void cardWithPromotion(List<CalculatedPaymentOption> options, Order order, BigDecimal originalValue) {
        for(PaymentMethod promoCard : order.applicablePromotions()){
            if(promoCard.isPoints()){ continue; }

            BigDecimal discountPercent = promoCard.getDiscountPercentage();
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal finalPrice = originalValue.multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);

            Map<PaymentMethod, BigDecimal> spent = new HashMap<>();
            spent.put(promoCard, finalPrice);

            options.add(new CalculatedPaymentOption(order,
                    finalPrice,
                    originalValue.subtract(finalPrice),
                    PaymentStrategyType.FULL_CARD_WITH_PROMOTION,
                    spent));
        }
    }

    private void fullPointsPayment(List<CalculatedPaymentOption> options, Order order, BigDecimal originalValue){
        if(pointsPaymentMethod != null){
            BigDecimal pointsOwnDiscountPercent = pointsPaymentMethod.getDiscountPercentage();
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(pointsOwnDiscountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal finalPrice = originalValue.multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);

            Map<PaymentMethod, BigDecimal> spent = new HashMap<>();
            spent.put(pointsPaymentMethod, finalPrice);

            options.add(new CalculatedPaymentOption(order,
                    finalPrice,
                    originalValue.subtract(finalPrice),
                    PaymentStrategyType.FULL_POINTS_OWN_DISCOUNT,
                    spent));
        }
    }

    private void minTenPctPointsPayment(List<CalculatedPaymentOption> options, Order order, BigDecimal originalValue){
        if (pointsPaymentMethod != null) {
            BigDecimal minPointsToActivateDiscount = originalValue.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal finalPriceWithGlobal10PctDiscount = originalValue.subtract(minPointsToActivateDiscount);

            if (minPointsToActivateDiscount.compareTo(BigDecimal.ZERO) > 0 &&
                    finalPriceWithGlobal10PctDiscount.compareTo(new BigDecimal("0.01")) >= 0) {

                for (PaymentMethod cardForRemainder : clientCardPaymentMethods) {
                    BigDecimal pointsSpentInScenario;
                    BigDecimal cardAmountInScenario;

                    BigDecimal preferredPointsToSpend = finalPriceWithGlobal10PctDiscount.subtract(new BigDecimal("0.01"));

                    if (preferredPointsToSpend.compareTo(minPointsToActivateDiscount) >= 0) {
                        pointsSpentInScenario = preferredPointsToSpend;
                        cardAmountInScenario = new BigDecimal("0.01");
                    } else {
                        pointsSpentInScenario = minPointsToActivateDiscount;
                        cardAmountInScenario = finalPriceWithGlobal10PctDiscount.subtract(pointsSpentInScenario);
                    }

                    if (cardAmountInScenario.compareTo(BigDecimal.ZERO) > 0) {
                        Map<PaymentMethod, BigDecimal> spent = new HashMap<>();
                        spent.put(pointsPaymentMethod, pointsSpentInScenario);
                        spent.put(cardForRemainder, cardAmountInScenario);

                        options.add(new CalculatedPaymentOption(
                                order,
                                finalPriceWithGlobal10PctDiscount,
                                minPointsToActivateDiscount,
                                PaymentStrategyType.PARTIAL_POINTS_10_PERCENT_GLOBAL_DISCOUNT,
                                spent
                        ));
                    }
                }
            }
        }
    }

    private void fullCardPaymentNoPromotion(List<CalculatedPaymentOption> options, Order order, BigDecimal originalValue){
        for (PaymentMethod card : clientCardPaymentMethods) {
            Map<PaymentMethod, BigDecimal> spent = new HashMap<>();
            spent.put(card, originalValue);
            options.add(new CalculatedPaymentOption(
                    order,
                    originalValue,
                    BigDecimal.ZERO,
                    PaymentStrategyType.FULL_PAYMENT_NO_PROMOTION,
                    spent
            ));
        }
    }
}

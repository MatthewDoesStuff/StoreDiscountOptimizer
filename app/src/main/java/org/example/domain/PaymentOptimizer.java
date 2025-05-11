package org.example.domain;

import org.example.logic.DiscountCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class PaymentOptimizer {
    private final List<Order> allOrders;
    private final DiscountCalculator discountCalculator;
    private final Map<String, PaymentMethod> initialPaymentMethods;
    private final PaymentMethod pointsPaymentMethodGlobalReference;
    private final List<PaymentMethod> clientCardPaymentMethods;

    private BigDecimal bestTotalDiscountSoFar;
    private List<AppliedPayment> bestPaymentPlanSoFar;
    private BigDecimal pointsSpentInBestPlan;

    public PaymentOptimizer(List<Order> allOrders,
                            DiscountCalculator discountCalculator,
                            Map<String, PaymentMethod> allClientInitialPaymentMethods) {
        this.allOrders = allOrders;
        this.discountCalculator = discountCalculator;

        this.initialPaymentMethods = Map.copyOf(allClientInitialPaymentMethods);
        this.pointsPaymentMethodGlobalReference = this.initialPaymentMethods.get("PUNKTY");
        this.clientCardPaymentMethods = this.initialPaymentMethods.values().stream()
                .filter(paymentMethod -> paymentMethod != null && !paymentMethod.isPoints())
                .toList();
    }

    public List<AppliedPayment> findOptimalPaymentPlan(){
        bestTotalDiscountSoFar = BigDecimal.valueOf(-1);
        bestPaymentPlanSoFar = new ArrayList<>();
        pointsSpentInBestPlan = BigDecimal.valueOf(Long.MAX_VALUE);

        Map<String, BigDecimal> initialLimits = new HashMap<>();
        for(PaymentMethod pm : initialPaymentMethods.values()) {
            initialLimits.put(pm.getId(), pm.getMethodLimit());
        }

        solveRecursively(0, initialLimits, BigDecimal.ZERO, new ArrayList<>());
        return bestPaymentPlanSoFar;
    }

    private void solveRecursively(
                                    int orderIndex,
                                    Map<String, BigDecimal> currentLimits,
                                    BigDecimal currentAccumulatedDiscount,
                                    List<AppliedPayment> currentPath){
        if(orderIndex == allOrders.size()){
            BigDecimal totalPointsSpentOnThisPath = calculateTotalPointsSpentFromPath(currentPath);

            if(currentAccumulatedDiscount.compareTo(bestTotalDiscountSoFar) > 0){
                bestTotalDiscountSoFar = currentAccumulatedDiscount;
                bestPaymentPlanSoFar = new ArrayList<>(currentPath);
                pointsSpentInBestPlan = totalPointsSpentOnThisPath;
            }
            else if (currentAccumulatedDiscount.compareTo(bestTotalDiscountSoFar) == 0){
                if(totalPointsSpentOnThisPath.compareTo(pointsSpentInBestPlan) > 0){
                    bestPaymentPlanSoFar = new ArrayList<>(currentPath);
                    pointsSpentInBestPlan = totalPointsSpentOnThisPath;
                }
            }
            return;
        }

        Order currentOrder = allOrders.get(orderIndex);
        if(currentOrder.value().compareTo(BigDecimal.ZERO) <= 0){
            currentPath.add(new AppliedPayment(
                    currentOrder.id(),
                    BigDecimal.ZERO,
                    PaymentStrategyType.FULL_PAYMENT_NO_PROMOTION,
                    Collections.emptyMap()));
            solveRecursively(orderIndex + 1, currentLimits, currentAccumulatedDiscount, currentPath);
            currentPath.removeLast();
            return;
        }

        List<CalculatedPaymentOption> optionsForThisOrder = discountCalculator.calculateOptionsForOrder(currentOrder);

        if(optionsForThisOrder.isEmpty()){
            return;
        }

        for(CalculatedPaymentOption option : optionsForThisOrder){
            List<Map<PaymentMethod, BigDecimal>> possibleCombinations =
                    determineActualSpentAmountsAndFeasibility(option, currentOrder, currentLimits);
            for(Map<PaymentMethod, BigDecimal> actualSpentAmounts : possibleCombinations){
                if(actualSpentAmounts.isEmpty()){
                    continue;
                }

                Map<String, BigDecimal> newLimits = new HashMap<>(currentLimits);
                for(Map.Entry<PaymentMethod, BigDecimal> entry : actualSpentAmounts.entrySet()){
                    String methodId = entry.getKey().getId();
                    BigDecimal amountToSpend = entry.getValue();
                    BigDecimal newLimit = newLimits.get(methodId).subtract(amountToSpend);
                    newLimits.put(methodId, newLimit);
                }

                currentPath.add(new AppliedPayment(
                        currentOrder.id(),
                        option.finalPrice(),
                        option.paymentStrategyType(),
                        actualSpentAmounts
                ));

                solveRecursively(orderIndex + 1,
                        newLimits,
                        currentAccumulatedDiscount.add(option.discountAmount()),
                        currentPath);

                currentPath.removeLast();
            }
        }
    }

    private List<Map<PaymentMethod, BigDecimal>> determineActualSpentAmountsAndFeasibility(
            CalculatedPaymentOption option,
            Order order,
            Map<String, BigDecimal> currentLimits) {

            List<Map<PaymentMethod, BigDecimal>> possibleCombinations = new ArrayList<>();

            switch(option.paymentStrategyType()){
                case FULL_CARD_WITH_PROMOTION:
                case FULL_POINTS_OWN_DISCOUNT:
                case FULL_PAYMENT_NO_PROMOTION:
                    Map<PaymentMethod, BigDecimal> proposedSpentAmount = option.spent();
                    boolean possible = true;
                    for(Map.Entry<PaymentMethod, BigDecimal> entry : proposedSpentAmount.entrySet()){
                        PaymentMethod paymentMethod = entry.getKey();
                        BigDecimal amountNeeded = entry.getValue();
                        if(currentLimits.getOrDefault(paymentMethod.getId(), BigDecimal.ZERO).compareTo(amountNeeded) < 0){
                            possible = false;
                            break;
                        }
                    }
                    if(possible && !proposedSpentAmount.isEmpty()){
                        possibleCombinations.add(new HashMap<>(proposedSpentAmount));
                    }
                    break;
                case PARTIAL_POINTS_10_PERCENT_GLOBAL_DISCOUNT:
                    BigDecimal finalPrice = option.finalPrice();
                    BigDecimal minPointsRequired = order.value().multiply(BigDecimal.valueOf(0.10)
                                                                    .setScale(2, RoundingMode.HALF_UP));
                    BigDecimal pointsAvailable = currentLimits.getOrDefault(pointsPaymentMethodGlobalReference.getId(), BigDecimal.ZERO);

                    if(pointsAvailable.compareTo(minPointsRequired) < 0){
                        break;
                    }

                    BigDecimal maxPointsToSpendLeavingMinimalAmountForCard = finalPrice.subtract(new BigDecimal("0.01"));
                    if(maxPointsToSpendLeavingMinimalAmountForCard.compareTo(BigDecimal.ZERO) < 0){
                        maxPointsToSpendLeavingMinimalAmountForCard = BigDecimal.ZERO;
                    }

                    BigDecimal actualPointsToSpend = pointsAvailable.min(maxPointsToSpendLeavingMinimalAmountForCard);

                    if(actualPointsToSpend.compareTo(minPointsRequired) < 0){
                        actualPointsToSpend = minPointsRequired;
                    }

                    BigDecimal cardAmount = finalPrice.subtract(actualPointsToSpend);

                    if(cardAmount.compareTo(BigDecimal.ZERO) <= 0){
                        break;
                    }

                    for(PaymentMethod card : clientCardPaymentMethods){
                        if(currentLimits.getOrDefault(card.getId(), BigDecimal.ZERO).compareTo(cardAmount) >= 0){
                            Map<PaymentMethod, BigDecimal> currentRealization = new HashMap<>();
                            currentRealization.put(pointsPaymentMethodGlobalReference, actualPointsToSpend);
                            currentRealization.put(card, cardAmount);
                            possibleCombinations.add(currentRealization);
                        }
                    }
                    break;
            }
            return possibleCombinations;
    }

    private BigDecimal calculateTotalPointsSpentFromPath(List<AppliedPayment> path){
        BigDecimal total = BigDecimal.ZERO;
        if(path == null){
            return total;
        }
        for(AppliedPayment payment : path){
            BigDecimal pointsInPayment = payment.actualAmountSpent().getOrDefault(pointsPaymentMethodGlobalReference, BigDecimal.ZERO);
            total = total.add(pointsInPayment);
        }
        return total;
    }
}

package org.example;

import org.example.domain.AppliedPayment;
import org.example.domain.Order;
import org.example.domain.PaymentMethod;
import org.example.domain.PaymentOptimizer;
import org.example.loader.OrdersLoader;
import org.example.loader.PaymentMethodsLoader;
import org.example.logic.DiscountCalculator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args)
    {
        if(args.length < 2) {
            System.err.println("Usage: java -jar app.jar <orders.json> <paymentmethods.json>");
            System.exit(1);
            return;
        }

        String ordersFilePath = args[0];
        String paymentMethodsFilePath = args[1];

        PaymentMethodsLoader paymentMethodsLoader = new PaymentMethodsLoader();
        OrdersLoader ordersLoader = new OrdersLoader();

        try{
            Map<String, PaymentMethod> allPaymentMethods = paymentMethodsLoader.loadPaymentMethods(paymentMethodsFilePath);
            List<Order> orders = ordersLoader.loadOrders(ordersFilePath, allPaymentMethods);

            if(orders.isEmpty()) {
                return;
            }

            BigDecimal totalOrderValueToPay = orders.stream()
                                            .map(Order::value)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            if(totalOrderValueToPay.compareTo(BigDecimal.ZERO) > 0 && allPaymentMethods.isEmpty()) {
                System.out.println("No payment methods available.");
                return;
            }

            if(totalOrderValueToPay.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("All orders have zero value.");
                return;
            }

            PaymentMethod pointsMethod = allPaymentMethods.get("PUNKTY");
            List<PaymentMethod> clientCards = allPaymentMethods.values().stream()
                                            .filter(pm -> pm != null && !pm.isPoints())
                                            .toList();

            DiscountCalculator discountCalculator = new DiscountCalculator(pointsMethod, clientCards);
            PaymentOptimizer optimizer = new PaymentOptimizer(orders, discountCalculator, allPaymentMethods);

            List<AppliedPayment> optimalPaymentPlan = optimizer.findOptimalPaymentPlan();

            if(optimalPaymentPlan.isEmpty() && totalOrderValueToPay.compareTo(BigDecimal.ZERO) > 0) {
                System.err.println("Could not find payment plan for orders.");
            }

            Map<String, BigDecimal> aggregatedExpenses = aggregateExpenses(optimalPaymentPlan);
            printResult(aggregatedExpenses);


        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            System.exit(2);
        } catch (IllegalArgumentException e){
            System.err.println("Invalid argument: " + e.getMessage());
            System.exit(3);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            System.exit(4);
        }
    }

    private static Map<String, BigDecimal> aggregateExpenses(List<AppliedPayment> paymentPlan) {
        Map<String, BigDecimal> totalSpentByMethod = new HashMap<>();
        if(paymentPlan != null){
            for(AppliedPayment payment : paymentPlan){
                if(payment.actualAmountSpent() != null){
                    for(Map.Entry<PaymentMethod, BigDecimal> entry : payment.actualAmountSpent().entrySet()){
                        String methodId = entry.getKey().getId();
                        BigDecimal amount = entry.getValue();
                        totalSpentByMethod.merge(methodId, amount, BigDecimal::add);
                    }
                }
            }
        }
        return totalSpentByMethod;
    }

    private static void printResult(Map<String, BigDecimal> aggregatedExpenses) {
        for(Map.Entry<String, BigDecimal> entry : aggregatedExpenses.entrySet()){
            System.out.println(entry.getKey() + " " + entry.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
    }
}

package org.example.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.Order;
import org.example.domain.PaymentMethod;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrdersLoader {
    private final ObjectMapper objectMapper;

    public OrdersLoader() {
        objectMapper = new ObjectMapper();
    }

    public List<Order> loadOrders(String filePath, Map<String, PaymentMethod> availablePaymentMethods) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found or it's not a file: " + filePath);
        }

        List<RawOrder> rawOrders = objectMapper.readValue(file, new TypeReference<List<RawOrder>>() {});
        return rawOrders.stream()
                .map(rawOrder -> transformToOrder(rawOrder, availablePaymentMethods))
                .collect(Collectors.toList());
    }

    private Order transformToOrder(RawOrder rawOrder, Map<String, PaymentMethod> availablePaymentMethods) {

        BigDecimal orderValue;
        try{
            orderValue = new BigDecimal(rawOrder.value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value format for Order ID: " + rawOrder.id, e);
        }

        List<PaymentMethod> resolvedPromotions = new ArrayList<>();
        if(rawOrder.promotions != null && !rawOrder.promotions.isEmpty()) {
            for(var promoId : rawOrder.promotions) {
                if(promoId == null || promoId.trim().isEmpty()) {
                    continue;
                }
                PaymentMethod paymentMethod = availablePaymentMethods.get(promoId);
                if(paymentMethod != null) {
                    resolvedPromotions.add(paymentMethod);
                }
            }
        }
        return new Order(rawOrder.id, orderValue, resolvedPromotions);
    }

    static class RawOrder {
        public String id;
        public String value;
        public List<String> promotions;

        public RawOrder() {}
    }
}

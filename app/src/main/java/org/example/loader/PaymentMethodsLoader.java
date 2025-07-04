package org.example.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.PaymentMethod;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PaymentMethodsLoader {
    private final ObjectMapper objectMapper;

    public PaymentMethodsLoader() {
        objectMapper = new ObjectMapper();
    }

    public Map<String, PaymentMethod> loadPaymentMethods(String filePath) throws IOException {

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found or it's not a file: " + filePath);
        }

        List<RawPaymentMethod> rawPaymentMethods = objectMapper.readValue(file, new TypeReference<>() {});

        return rawPaymentMethods.stream()
                .map(this::transformToPaymentMethod)
                .collect(Collectors.toMap(PaymentMethod::getId, paymentMethod -> paymentMethod, (PM1, PM2) -> {
                    throw new IllegalArgumentException("Duplicate PaymentMethod ID: " + PM1.getId());
                }));
    }

    private PaymentMethod transformToPaymentMethod(RawPaymentMethod rawPaymentMethod) {
        BigDecimal discount;
        try{
            discount = new BigDecimal(rawPaymentMethod.discount);
        } catch (NumberFormatException e){
            throw new IllegalArgumentException("Invalid discount format for PaymentMethod ID: " + rawPaymentMethod.id, e);
        }

        BigDecimal limit;
        try{
            limit = new BigDecimal(rawPaymentMethod.limit);
        } catch (NumberFormatException e){
            throw new IllegalArgumentException("Invalid limit format for PaymentMethod ID: " + rawPaymentMethod.id, e);
        }

        return new PaymentMethod(rawPaymentMethod.id, discount, limit);
    }

    static class RawPaymentMethod {
        public String id;
        public String discount;
        public String limit;

        public RawPaymentMethod(){}
    }
}

package org.example.loader

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.domain.Order
import org.example.domain.PaymentMethod
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class OrdersLoaderTest extends Specification {
    ObjectMapper objectMapper
    OrdersLoader loader

    @TempDir
    Path temporaryFolder

    @Shared
    Map<String, PaymentMethod> availablePaymentMethods

    def setupSpec(){
        availablePaymentMethods = new HashMap<>()
        availablePaymentMethods.put("mZysk", new PaymentMethod("mZysk", new BigDecimal("10"), new BigDecimal("180.00")))
        availablePaymentMethods.put("BosBankrut", new PaymentMethod("BosBankrut", new BigDecimal("5"), new BigDecimal("200.00")))
    }

    def setup() {
        objectMapper = new ObjectMapper()
        loader = new OrdersLoader()
    }

    def "should load orders successfully and resolve existing promotions"() {
        given:
        String jsonContent = """
        [
            {"id": "ORDER1", "value": "150.00", "promotions": ["mZysk", "NonExistentPromo"]},
            {"id": "ORDER2", "value": "200.00", "promotions": ["BosBankrut"]},
            {"id": "ORDER3", "value": "50.00"} 
        ]
        """
        Path tempFile = temporaryFolder.resolve("test-orders.json")
        Files.writeString(tempFile, jsonContent)

        when:
        List<Order> result = loader.loadOrders(tempFile.toString(), availablePaymentMethods)

        then:
        result != null
        result.size() == 3

        and: "check ORDER1"
        def order1 = result.find { it.id() == "ORDER1" }
        order1 != null
        order1.value() == new BigDecimal("150.00").setScale(2)
        order1.applicablePromotions().size() == 1
        order1.applicablePromotions().get(0).getId() == "mZysk"

        and: "check ORDER2"
        def order2 = result.find { it.id() == "ORDER2" }
        order2 != null
        order2.value() == new BigDecimal("200.00").setScale(2)
        order2.applicablePromotions().size() == 1

        and: "check ORDER3 (no promotions field in JSON)"
        def order3 = result.find { it.id() == "ORDER3" }
        order3 != null
        order3.value() == new BigDecimal("50.00").setScale(2)
        order3.applicablePromotions() != null
        order3.applicablePromotions().isEmpty()
    }

    def "should handle empty promotions array in order JSON"() {
        given:
        String jsonContent = """
        [
            {"id": "ORDER_EMPTY_PROMO", "value": "100.00", "promotions": []}
        ]
        """
        Path tempFile = temporaryFolder.resolve("empty-promotions.json")
        Files.writeString(tempFile, jsonContent)

        when:
        List<Order> result = loader.loadOrders(tempFile.toString(), availablePaymentMethods)

        then:
        result.size() == 1
        result.get(0).id() == "ORDER_EMPTY_PROMO"
        result.get(0).applicablePromotions().isEmpty()
    }

    def "should return empty list for empty JSON array (orders)"() {
        given:
        String jsonContent = "[]"

        Path tempFile = temporaryFolder.resolve("empty-orders.json")
        Files.writeString(tempFile, jsonContent)

        when:
        List<Order> result = loader.loadOrders(tempFile.toString(), availablePaymentMethods)

        then:
        result != null
        result.isEmpty()
    }

    def "should throw IllegalArgumentException for invalid order value format"() {
        given:
        String jsonContent = """
        [
            {"id": "BAD_VALUE", "value": "not-a-number"}
        ]
        """
        Path tempFile = temporaryFolder.resolve("invalid-orders.json")
        Files.writeString(tempFile, jsonContent)

        when:
        loader.loadOrders(tempFile.toString(), availablePaymentMethods)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.getMessage().contains("Invalid value format for Order ID: BAD_VALUE")
    }
}

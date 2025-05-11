package org.example.loader

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.domain.PaymentMethod
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

class PaymentMethodsLoaderTest extends Specification {

    ObjectMapper objectMapper
    PaymentMethodsLoader loader

    @TempDir
    Path temporaryFolder

    def setup() {
        objectMapper = new ObjectMapper()
        loader = new PaymentMethodsLoader()
    }

    def "should load payment methods successfully from a temporary file"() {
        given: "JSON content to be written to a file"
        String jsonContent = """
        [
            {"id": "PUNKTY", "discount": "15", "limit": "100.00"},
            {"id": "mZysk", "discount": "10", "limit": "180.00"}
        ]
        """
        and: "a temporary file with the JSON content"
        Path tempFile = temporaryFolder.resolve("test-payments.json")
        Files.writeString(tempFile, jsonContent)

        when: "the loader reads from the temporary file path"
        Map<String, PaymentMethod> result = loader.loadPaymentMethods(tempFile.toString())

        then: "the payment methods are loaded correctly"
        result != null
        result.size() == 2

        and: "PUNKTY method is correct"
        def punkty = result.get("PUNKTY")
        punkty != null
        punkty.getId() == "PUNKTY"
        punkty.getDiscountPercentage() == new BigDecimal("15")
        punkty.getMethodLimit() == new BigDecimal("100.00").setScale(2)
    }

    def "should return empty map for empty JSON array"() {
        given:
        String jsonContent = "[]"
        Path tempFile = temporaryFolder.resolve("emptyPayments.json")
        Files.writeString(tempFile, jsonContent)

        when:
        Map<String, PaymentMethod> result = loader.loadPaymentMethods(tempFile.toString())

        then:
        result != null
        result.isEmpty()
    }

    def "should throw IllegalArgumentException for duplicate IDs"() {
        given:
        String jsonContent = """
        [
            {"id": "DUPLICATE", "discount": "10", "limit": "100.00"},
            {"id": "UNIQUE", "discount": "5", "limit": "50.00"},
            {"id": "DUPLICATE", "discount": "15", "limit": "150.00"}
        ]
        """
        Path tempFile = temporaryFolder.resolve("duplicatePayments.json")
        Files.writeString(tempFile, jsonContent)

        when:
        loader.loadPaymentMethods(tempFile.toString())

        then:
        def exception = thrown(IllegalArgumentException)
        exception.getMessage().contains("Duplicate PaymentMethod ID: DUPLICATE")
    }

    @Unroll
    def "should throw IllegalArgumentException for invalid field format in payment method JSON"() {
        given:
        String jsonContent = """
        [
            {"id": "$id", "discount": "$discount", "limit": "$limit"}
        ]
        """
        Path tempFile = temporaryFolder.resolve("invalidPayments.json")
        Files.writeString(tempFile, jsonContent)

        when:
        loader.loadPaymentMethods(tempFile.toString())

        then:
        def exception = thrown(IllegalArgumentException)
        exception.getMessage().contains(expectedMessageFragment)

        where:
        fieldFormat        | id             | discount  | limit      | expectedMessageFragment
        "discount format"  | "BAD_DISCOUNT" | "abc"     | "100.00"   | "Invalid discount format for PaymentMethod ID: BAD_DISCOUNT"
        "limit format"     | "BAD_LIMIT"    | "10"      | "xyz"      | "Invalid limit format for PaymentMethod ID: BAD_LIMIT"
    }
}

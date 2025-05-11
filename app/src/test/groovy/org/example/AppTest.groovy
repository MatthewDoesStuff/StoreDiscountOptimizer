package org.example

import spock.lang.Specification
import spock.lang.TempDir

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class AppTest extends Specification {

    @TempDir
    Path tempDir

    private PrintStream originalOut
    private PrintStream originalErr
    private ByteArrayOutputStream outContent
    private ByteArrayOutputStream errContent

    def setup() {
        originalOut = System.out
        originalErr = System.err
        outContent = new ByteArrayOutputStream()
        errContent = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8))
        System.setErr(new PrintStream(errContent, true, StandardCharsets.UTF_8))
    }

    def cleanup() {
        System.setOut(originalOut)
        System.setErr(originalErr)
        try {
            outContent.close()
            errContent.close()
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

    def "should produce correct aggregated output for the PDF example data"() {
        given: "the example orders.json content"
        String ordersJson = """
        [
            {"id": "ORDER1", "value": "100.00", "promotions": ["mZysk"]},
            {"id": "ORDER2", "value": "200.00", "promotions": ["BosBankrut"]},
            {"id": "ORDER3", "value": "150.00", "promotions": ["mZysk", "BosBankrut"]},
            {"id": "ORDER4", "value": "50.00"}
        ]
        """
        Path ordersFile = tempDir.resolve("orders_example.json")
        Files.writeString(ordersFile, ordersJson, StandardCharsets.UTF_8)

        and: "the example paymentmethods.json content"
        String paymentsJson = """
        [
            {"id": "PUNKTY", "discount": "15", "limit": "100.00"},
            {"id": "mZysk", "discount": "10", "limit": "180.00"},
            {"id": "BosBankrut", "discount": "5", "limit": "200.00"}
        ]
        """
        Path paymentsFile = tempDir.resolve("paymentmethods_example.json")
        Files.writeString(paymentsFile, paymentsJson, StandardCharsets.UTF_8)

        when: "the main application is run with these files"
        App.main([ordersFile.toString(), paymentsFile.toString()] as String[])

        then: "capture and print outputs, then assert"
        String stdOutputString = outContent.toString(StandardCharsets.UTF_8)
        String errOutputString = errContent.toString(StandardCharsets.UTF_8)

        System.err.println("--- CAPTURED STDOUT (from App.main) ---")
        System.err.println(stdOutputString.isEmpty() ? "<stdout is empty>" : stdOutputString)
        System.err.println("--- END CAPTURED STDOUT ---")

        System.err.println("--- CAPTURED STDERR (from App.main) ---")
        System.err.println(errOutputString.isEmpty() ? "<stderr is empty>" : errOutputString)
        System.err.println("--- END CAPTURED STDERR ---")

        List<String> expectedOutputLines = [
                "mZysk 165.00",
                "BosBankrut 190.00",
                "PUNKTY 100.00"
        ]

        List<String> actualOutputLines = stdOutputString
                .trim()
                .split(/\r?\n/)
                .findAll { !it.isEmpty() }
                .collect()

        actualOutputLines.size() == expectedOutputLines.size()
        new HashSet<>(actualOutputLines) == new HashSet<>(expectedOutputLines)

        if (new HashSet<>(actualOutputLines) != new HashSet<>(expectedOutputLines)) {
            System.err.println("ASSERTION FAILED: Output mismatch!")
            System.err.println("Expected output (any order):")
            expectedOutputLines.each { System.err.println(it) }
            System.err.println("\nActual output (from App.main's stdout):")
            actualOutputLines.each { System.err.println(it) }
        }
    }
}

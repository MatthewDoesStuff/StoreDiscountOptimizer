Online Store Payment Optimizer

# Project Description

A Java console application designed to optimize the payment process for orders in an online store. For a given list of orders, available customer payment methods (along with their limits and discounts), and defined promotions, the algorithm selects the optimal payment method for each order. The main goal is to maximize the total discount obtained by the customer while ensuring all orders are fully paid and preferentially using loyalty points, provided it does not reduce the due discount.

# Technologies

* **Language:** Java 21
* **Build Tool:** Gradle
* **Libraries:**
    * Jackson Databind
    * Spock Framework
 

## Project Structure
Ocado0525
├── app
│   ├── build
│   ├── src
│   │   ├── main
│   │   │   ├── java
│   │   │   │   └── org.example
│   │   │   │       ├── domain
│   │   │   │       │   ├── AppliedPayment.java
│   │   │   │       │   ├── CalculatedPaymentOption.java
│   │   │   │       │   ├── Order.java
│   │   │   │       │   ├── PaymentMethod.java
│   │   │   │       │   ├── PaymentOptimizer.java
│   │   │   │       │   └── PaymentStrategyType.java
│   │   │   │       ├── loader
│   │   │   │       │   ├── OrdersLoader.java
│   │   │   │       │   └── PaymentMethodsLoader.java
│   │   │   │       ├── logic
│   │   │   │       │   └── DiscountCalculator.java
│   │   │   │       └── App.java
│   │   │   └── resources
│   │   └── test
│   │       ├── groovy
│   │       │   └── org.example
│   │       │       ├── domain
│   │       │       │   ├── OrderTest.groovy
│   │       │       │   └── PaymentMethodTest.groovy
│   │       │       ├── loader
│   │       │       │   ├── OrdersLoaderTest.groovy
│   │       │       │   └── PaymentMethodsLoaderTest.groovy
│   │       │       ├── logic
│   │       │       │   ├── DiscountCalculatorTest.groovy
│   │       │       │   └── AppTest.groovy
│   │       └── resources
│   └── build.gradle
├── gradle
├── .gitattributes
├── .gitignore
├── gradlew
├── gradlew.bat
└── settings.gradle

# Building the Project

## Prerequisites

* JDK version 17 (or 21) installed.
* Build tool Gradle.

## Build Steps (using Gradle Wrapper)

1.  Clone the repository (if applicable).
2.  Open a terminal/command prompt in the project's root directory.
3.  Run the build command to create a "fat JAR" (JAR containing all dependencies):
    ```bash
    ./gradlew shadowJar
    ```

    The resulting JAR file will typically be in the `build/libs/` (for Gradle) or `target/` (for Maven) directory. The JAR file name may depend on the project configuration (e.g., `project-name-all.jar` or `project-name-1.0-SNAPSHOT-shaded.jar`).

# Running the Application

After building the application, run it from the command line, providing paths to two JSON files as arguments: the orders file and the payment methods file.

```bash
java -jar path/to/your/app-file.jar path/to/orders.json path/to/paymentmethods.json
```

# Example of JSON files

## orders.json
[
  {
    "id": "ORDER1",
    "value": "100.00",
    "promotions": ["mZysk"]
  },
  {
    "id": "ORDER2",
    "value": "200.00"
    // "promotions" field is optional
  }
  // ... more orders
]

## paymentmethods.json
[
  {
    "id": "PUNKTY",
    "discount": "15",
    "limit": "100.00"
  },
  {
    "id": "mZysk",
    "discount": "10",
    "limit": "180.00"
  }
  // ... more payment methods
]

# Output format

The application prints the total amount spent for each used payment method to standard output. Each line has the format:

<PAYMENT_METHOD_ID> <AMOUNT_SPENT>

The amount is formatted to two decimal places. The order of lines is arbitrary.

Example output:

mZysk 165.00
BosBankrut 190.00
PUNKTY 100.00
package org.example.loader

import spock.lang.Specification

class OrdersLoaderTest extends Specification {
    def "should return an empty list when no orders are available"() {
        given:
        def ordersLoader = new OrdersLoader()

        when:
        def orders = ordersLoader.load("src/test/resources/emptyOrders.json")

        then:
        orders != null
        orders.size() == 0
    }
}

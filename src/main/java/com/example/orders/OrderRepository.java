package com.example.orders;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private final Map<String, Order> orders = Map.of(
            "1", new Order("1", new BigDecimal("1500.00"), "NEW"),
            "2", new Order("2", new BigDecimal("299.90"), "PAID"));

    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }
}

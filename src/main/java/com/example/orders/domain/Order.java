package com.example.orders.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
    UUID id, String customerName, List<OrderItem> items, OrderStatus status, Instant createdAt) {}

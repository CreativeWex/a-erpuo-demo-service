package com.example.orders.api;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String customerName,
    List<OrderItemResponse> items,
    OrderStatus status,
    Instant createdAt) {

  static OrderResponse from(Order order) {
    return new OrderResponse(
        order.id(),
        order.customerName(),
        order.items().stream().map(OrderItemResponse::from).toList(),
        order.status(),
        order.createdAt());
  }
}

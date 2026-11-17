package com.example.orders.api;

import com.example.orders.domain.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(String productName, int quantity, BigDecimal price) {

  static OrderItemResponse from(OrderItem item) {
    return new OrderItemResponse(item.productName(), item.quantity(), item.price());
  }
}

package com.example.orders.service;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderItem;
import com.example.orders.domain.OrderStatus;
import com.example.orders.repository.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

  private final OrderRepository repository;
  private final Clock clock;

  public OrderService(OrderRepository repository) {
    this.repository = repository;
    this.clock = Clock.systemUTC();
  }

  public Order create(String customerName, List<OrderItem> items, OrderStatus status) {
    Order order =
        new Order(
            UUID.randomUUID(),
            customerName,
            List.copyOf(items),
            status == null ? OrderStatus.NEW : status,
            Instant.now(clock));
    return repository.save(order);
  }

  public List<Order> list() {
    return repository.findAll();
  }

  public Order get(UUID id) {
    return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
  }

  public Order replace(UUID id, String customerName, List<OrderItem> items, OrderStatus status) {
    OrderStatus newStatus = status == null ? OrderStatus.NEW : status;
    List<OrderItem> newItems = List.copyOf(items);
    return repository
        .replace(id, old -> new Order(old.id(), customerName, newItems, newStatus, old.createdAt()))
        .orElseThrow(() -> new OrderNotFoundException(id));
  }

  public void delete(UUID id) {
    if (!repository.deleteById(id)) {
      throw new OrderNotFoundException(id);
    }
  }
}

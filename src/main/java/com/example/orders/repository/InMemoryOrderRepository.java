package com.example.orders.repository;

import com.example.orders.domain.Order;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

  private record Entry(long sequence, Order order) {}

  private final Map<UUID, Entry> storage = new ConcurrentHashMap<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public Order save(Order order) {
    storage.put(order.id(), new Entry(sequence.incrementAndGet(), order));
    return order;
  }

  @Override
  public Optional<Order> findById(UUID id) {
    return Optional.ofNullable(storage.get(id)).map(Entry::order);
  }

  @Override
  public List<Order> findAll() {
    return storage.values().stream()
        .sorted(
            Comparator.comparing((Entry e) -> e.order().createdAt())
                .thenComparingLong(Entry::sequence))
        .map(Entry::order)
        .toList();
  }

  @Override
  public Optional<Order> replace(UUID id, UnaryOperator<Order> updater) {
    Entry updated =
        storage.computeIfPresent(id, (key, e) -> new Entry(e.sequence(), updater.apply(e.order())));
    return Optional.ofNullable(updated).map(Entry::order);
  }

  @Override
  public boolean deleteById(UUID id) {
    return storage.remove(id) != null;
  }
}

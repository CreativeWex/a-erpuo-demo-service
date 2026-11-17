package com.example.orders.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class OrderConcurrencyApiTest extends AbstractOrdersApiTest {

  private static final int TIMEOUT_SECONDS = 30;

  /** Запускает все задачи одновременно (по общему стартовому сигналу) и собирает результаты. */
  private <T> List<T> runConcurrently(List<Callable<T>> tasks) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
    try {
      CountDownLatch ready = new CountDownLatch(tasks.size());
      CountDownLatch start = new CountDownLatch(1);
      List<Future<T>> futures = new ArrayList<>();
      for (Callable<T> task : tasks) {
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return task.call();
                }));
      }
      assertThat(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<T> results = new ArrayList<>();
      for (Future<T> future : futures) {
        results.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      }
      return results;
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  @DisplayName("100 одновременных созданий дают ровно 100 заказов с уникальными id")
  void concurrentCreatesKeepAllOrders() throws Exception {
    int count = 100;
    List<Callable<Integer>> tasks = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String body = orderJson("Customer" + i, "[" + VALID_ITEM + "]");
      tasks.add(
          () ->
              mockMvc
                  .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                  .andReturn()
                  .getResponse()
                  .getStatus());
    }

    List<Integer> statuses = runConcurrently(tasks);

    assertThat(statuses).hasSize(count).containsOnly(201);
    JsonNode orders = listOrders();
    assertThat(orders).hasSize(count);
    Set<String> ids = new HashSet<>();
    orders.forEach(o -> ids.add(o.path("id").asText()));
    assertThat(ids).hasSize(count);
  }

  @Test
  @DisplayName("Одновременные PUT и DELETE одного заказа: нет 500, итоговое состояние согласовано")
  void concurrentReplaceAndDeleteStayConsistent() throws Exception {
    String id = createOrder(validOrderJson()).path("id").asText();
    int puts = 10;
    int deletes = 10;

    List<Callable<String>> tasks = new ArrayList<>();
    for (int i = 0; i < puts; i++) {
      String body =
          orderJson("Replaced" + i, "[{\"productName\":\"P" + i + "\",\"quantity\":1,\"price\":1}]");
      tasks.add(
          () ->
              "PUT:"
                  + mockMvc
                      .perform(
                          put("/orders/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                      .andReturn()
                      .getResponse()
                      .getStatus());
    }
    for (int i = 0; i < deletes; i++) {
      tasks.add(
          () ->
              "DELETE:"
                  + mockMvc.perform(delete("/orders/" + id)).andReturn().getResponse().getStatus());
    }

    List<String> results = runConcurrently(tasks);

    List<String> putResults = results.stream().filter(r -> r.startsWith("PUT:")).toList();
    List<String> deleteResults = results.stream().filter(r -> r.startsWith("DELETE:")).toList();
    assertThat(putResults).hasSize(puts).allMatch(r -> r.equals("PUT:200") || r.equals("PUT:404"));
    assertThat(deleteResults)
        .hasSize(deletes)
        .allMatch(r -> r.equals("DELETE:204") || r.equals("DELETE:404"));
    long successfulDeletes = deleteResults.stream().filter(r -> r.equals("DELETE:204")).count();
    assertThat(successfulDeletes).isLessThanOrEqualTo(1);

    MvcResult finalRead = mockMvc.perform(get("/orders/" + id)).andReturn();
    if (successfulDeletes == 1) {
      assertThat(finalRead.getResponse().getStatus()).isEqualTo(404);
      assertThat(listOrders()).isEmpty();
    } else {
      assertThat(finalRead.getResponse().getStatus()).isEqualTo(200);
      JsonNode order = JSON.readTree(finalRead.getResponse().getContentAsString());
      assertThat(order.path("id").asText()).isEqualTo(id);
      String name = order.path("customerName").asText();
      assertThat(name).matches("Replaced\\d|Alice");
      if (name.startsWith("Replaced")) {
        String suffix = name.substring("Replaced".length());
        assertThat(order.path("items").get(0).path("productName").asText()).isEqualTo("P" + suffix);
      }
    }
  }
}

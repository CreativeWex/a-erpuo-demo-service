package com.example.orders.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Общая основа API-тестов. Хранилище заказов — in-memory и общее на контекст, поэтому контекст
 * пересоздаётся перед каждым тестом: состояние между тестами не протекает.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
abstract class AbstractOrdersApiTest {

  protected static final ObjectMapper JSON = new ObjectMapper();

  protected static final String VALID_ITEM =
      "{\"productName\":\"Widget\",\"quantity\":2,\"price\":10.50}";

  @Autowired protected MockMvc mockMvc;

  protected static String orderJson(String customerName, String itemsJson) {
    return "{\"customerName\":\"" + customerName + "\",\"items\":" + itemsJson + "}";
  }

  protected static String orderJson(String customerName, String itemsJson, String status) {
    return "{\"customerName\":\""
        + customerName
        + "\",\"items\":"
        + itemsJson
        + ",\"status\":\""
        + status
        + "\"}";
  }

  protected static String validOrderJson() {
    return orderJson("Alice", "[" + VALID_ITEM + "]");
  }

  protected MvcResult postRaw(String body) throws Exception {
    return mockMvc
        .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
        .andReturn();
  }

  /** Создаёт заказ и требует 201; возвращает тело ответа. */
  protected JsonNode createOrder(String body) throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn();
    return JSON.readTree(result.getResponse().getContentAsString());
  }

  protected JsonNode listOrders() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/orders")).andExpect(status().isOk()).andReturn();
    return JSON.readTree(result.getResponse().getContentAsString());
  }

  protected void assertNoOrdersExist() throws Exception {
    JsonNode orders = listOrders();
    assertThat(orders.isArray()).isTrue();
    assertThat(orders).isEmpty();
  }
}

package com.example.orders.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class ErrorResponseFormatApiTest extends AbstractOrdersApiTest {

  private void assertErrorBody(MockHttpServletRequestBuilder request, int expectedStatus)
      throws Exception {
    MvcResult result = mockMvc.perform(request).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
    String raw = result.getResponse().getContentAsString();
    JsonNode body = JSON.readTree(raw);
    assertThat(body.path("status").asInt()).isEqualTo(expectedStatus);
    assertThat(body.path("error").asText()).isNotBlank();
    assertThat(body.path("message").isTextual()).isTrue();
    assertThat(body.has("trace")).isFalse();
    assertThat(body.has("stackTrace")).isFalse();
    assertThat(raw).doesNotContain("\\tat ").doesNotContain("at com.example").doesNotContain("at org.springframework");
  }

  @Test
  @DisplayName("400 на невалидное тело POST содержит status, error, message без стек-трейса")
  void validationErrorHasUnifiedFormat() throws Exception {
    assertErrorBody(
        post("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"customerName\":\"\",\"items\":[]}"),
        400);
  }

  @Test
  @DisplayName("400 на битый JSON содержит status, error, message без стек-трейса")
  void malformedJsonErrorHasUnifiedFormat() throws Exception {
    assertErrorBody(
        post("/orders").contentType(MediaType.APPLICATION_JSON).content("{broken"), 400);
  }

  @Test
  @DisplayName("400 на не-UUID в пути содержит status, error, message без стек-трейса")
  void nonUuidErrorHasUnifiedFormat() throws Exception {
    assertErrorBody(get("/orders/not-a-uuid"), 400);
    assertErrorBody(delete("/orders/not-a-uuid"), 400);
  }

  @Test
  @DisplayName("404 на GET, PUT и DELETE несуществующего заказа содержит status, error, message")
  void notFoundErrorHasUnifiedFormat() throws Exception {
    UUID unknown = UUID.randomUUID();
    assertErrorBody(get("/orders/" + unknown), 404);
    assertErrorBody(delete("/orders/" + unknown), 404);
    assertErrorBody(
        put("/orders/" + unknown)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validOrderJson()),
        404);
  }
}

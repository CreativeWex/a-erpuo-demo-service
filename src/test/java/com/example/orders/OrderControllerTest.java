package com.example.orders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Клиент получает данные существующего заказа")
    void returnsExistingOrder() throws Exception {
        given(orderRepository.findById("42"))
                .willReturn(Optional.of(new Order("42", new BigDecimal("1500.50"), "NEW")));

        mockMvc.perform(get("/orders/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("42"))
                .andExpect(jsonPath("$.amount").value(1500.50))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @DisplayName("Клиент узнаёт, что запрошенного заказа не существует")
    void returnsNotFoundForUnknownOrder() throws Exception {
        given(orderRepository.findById("unknown")).willReturn(Optional.empty());

        mockMvc.perform(get("/orders/unknown"))
                .andExpect(status().isNotFound());
    }
}

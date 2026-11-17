package com.example.orders;

import java.math.BigDecimal;

public record Order(String id, BigDecimal amount, String status) {
}

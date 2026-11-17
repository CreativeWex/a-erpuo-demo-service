package com.example.orders.api;

public record ApiError(int status, String error, String message) {}

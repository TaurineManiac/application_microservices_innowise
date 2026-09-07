package org.example.payment_service.dto;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.example.payment_service.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentResponseEvent {
    @NotNull
    private Long id;
    @NotNull
    private UUID orderPublicId;
    @NotNull
    private UUID userPublicId;
    @NotNull
    private PaymentStatus status;
    @Digits(integer = 17, fraction = 2)
    @NotNull
    private BigDecimal amount;
    @NotNull
    private LocalDateTime createdAt;
    @NotNull
    private LocalDateTime updatedAt;

}

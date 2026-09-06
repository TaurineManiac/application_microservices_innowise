package org.example.payment_service.dto;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import org.example.payment_service.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentResponseEvent {
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private UUID orderPublicId;

    @Column(nullable = false)
    private UUID userPublicId;

    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    @Digits(integer = 17, fraction = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

}

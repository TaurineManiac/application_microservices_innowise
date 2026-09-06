package org.example.payment_service.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentRequestEvent {
    @Column(nullable = false)
    private UUID orderPublicId;

    @Column(nullable = false)
    private UUID userPublicId;

    @Digits(integer = 17, fraction = 2)
    @Column(nullable = false)
    private BigDecimal amount;
}

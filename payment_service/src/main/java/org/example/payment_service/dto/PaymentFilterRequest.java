package org.example.payment_service.dto;

import lombok.*;
import org.example.payment_service.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFilterRequest {
    private UUID userPublicId;
    private UUID orderPublicId;
    private PaymentStatus paymentStatus;

    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;

    private LocalDateTime updatedFrom;
    private LocalDateTime updatedTo;

    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}

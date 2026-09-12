package org.example.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.order_service.enums.PaymentStatus;


import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusEvent {
    private UUID orderPublicId;
    private UUID userPublicId;
    private PaymentStatus paymentStatus;
    private BigDecimal amount;
}
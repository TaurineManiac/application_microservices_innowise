package org.example.payment_service.dto;

import lombok.*;
import org.example.payment_service.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentFilterRequest {
    private UUID publicUserId;

    private UUID publicOrderId;

    private PaymentStatus paymentStatus;

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}

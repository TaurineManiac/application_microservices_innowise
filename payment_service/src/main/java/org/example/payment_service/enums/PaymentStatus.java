package org.example.payment_service.enums;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELED
}

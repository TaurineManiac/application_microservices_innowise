package org.example.order_service.enums;

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

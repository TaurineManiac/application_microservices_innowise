package org.example.order_service.dto;

import lombok.*;
import org.example.order_service.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderFilterRequest {
    private UUID userPublicId;
    private OrderStatus status;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}

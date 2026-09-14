package org.example.order_service.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestEvent {
    @NotNull
    private UUID orderPublicId;

    @NotNull
    private UUID userPublicId;

    @Digits(integer = 17, fraction = 2)
    @NotNull
    @Positive
    private BigDecimal amount;
}

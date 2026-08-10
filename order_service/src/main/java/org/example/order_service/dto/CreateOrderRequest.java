package org.example.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "User public ID is required")
    private UUID userPublicId;

    @Valid
    @NotNull(message = "Order items cannot be null")
    private List<CreateOrderItemRequest> items;
}
package org.example.order_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.order_service.enums.ItemStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItemStatusRequest {

    @NotNull(message = "Status is required")
    private ItemStatus status;
}
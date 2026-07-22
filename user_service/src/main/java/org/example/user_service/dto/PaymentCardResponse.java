package org.example.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCardResponse {
    private Long id;
    private String number;
    private String holder;
    private LocalDate expirationDate;
    private Boolean active;
}

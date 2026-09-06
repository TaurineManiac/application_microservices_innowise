package org.example.payment_service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public enum AmountConstraints {
    MAX_SCALE(2),
    MAX_PRECISION(19);

    private final Integer value;
}

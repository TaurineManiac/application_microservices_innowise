package org.example.authentication_service.enums;

import lombok.Getter;

@Getter
public enum Constraint {
    MAX_RETRIES_TO_CHECK_ADMIN(10),
    RETRY_DELAY_MS_TO_CHECK_ADMIN(5000);


    private final int value;

    Constraint(int value) {
        this.value = value;
    }
}

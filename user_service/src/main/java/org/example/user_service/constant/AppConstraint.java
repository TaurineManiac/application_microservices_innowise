package org.example.user_service.constant;

import lombok.Getter;

@Getter
public enum AppConstraint {
    MAX_CARDS_PER_USER(5),
    MAX_RETRIES_FOR_CARD_NUMBER(100),
    MAX_RETRIES_FOR_PUBLIC_ID(10),
    CARD_EXPIRY_YEARS(5),
    TIME_TO_LIVE_MINUTES_CACHE(10);

    private final int value;

    AppConstraint(int value) {
        this.value = value;
    }

}
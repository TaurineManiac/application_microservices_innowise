package org.example.payment_service.util;

import org.example.payment_service.enums.PaymentStatus;

import java.util.concurrent.ThreadLocalRandom;

public final class PaymentStatusGeneratorUtil {

    public static PaymentStatus generateRandomStatus() {
        int randomNumber = ThreadLocalRandom.current().nextInt();
        return (randomNumber % 2 == 0) ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
    }

}

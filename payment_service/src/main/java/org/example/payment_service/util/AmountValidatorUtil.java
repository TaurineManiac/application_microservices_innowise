package org.example.payment_service.util;

import org.example.payment_service.enums.AmountConstraints;

import java.math.BigDecimal;

public class AmountValidatorUtil {

    private static boolean isValidAmount(BigDecimal amount) {
        if (amount == null) {
            return false;
        }

        if (amount.scale() > AmountConstraints.MAX_SCALE.getValue()) {
            return false;
        }

        int precision = amount.precision();
        return precision <= AmountConstraints.MAX_PRECISION.getValue();
    }

    public static BigDecimal isValidAmountOrThrow(BigDecimal amount) {
        if (!isValidAmount(amount)) {
            throw new IllegalArgumentException(
                    "Amount must have at most " + AmountConstraints.MAX_SCALE.getValue() + " decimal places and " +
                            "total digits " + AmountConstraints.MAX_PRECISION.getValue() + ". Provided: " + amount
            );
        }
        return amount;
    }
}
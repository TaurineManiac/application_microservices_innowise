package org.example.user_service.util;

import java.security.SecureRandom;

public class CardNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    // Now I didn't add stronger algorithm for generating 0_0

    public static String generateRandomCardNumber() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}

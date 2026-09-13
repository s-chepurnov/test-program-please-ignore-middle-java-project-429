package io.hexlet.flightbooking.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
* Генерирует 6-символьные коды брони.
 */

@Component
public class BookingCodeGenerator {

    private static final char[] ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private static final int LENGTH = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        var sb = new StringBuilder(LENGTH);
        for (var i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}

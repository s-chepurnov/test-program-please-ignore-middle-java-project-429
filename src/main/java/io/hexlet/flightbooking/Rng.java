package io.hexlet.flightbooking;

/**
 * Детерминированный генератор (линейный конгруэнтный). Нужен, чтобы набор рейсов был одинаковым
 * между запусками: со {@code Random} отладка превращается в угадывание, а проверка становится
 * флаки.
 */
public final class Rng {

    private static final long MODULO = 0xFFFFFFFFL;

    private long seed;

    public Rng(long seed) {
        this.seed = seed & MODULO;
    }

    public double next() {
        seed = (seed * 1664525 + 1013904223) & MODULO;

        return (double) seed / MODULO;
    }
}

package io.hexlet.flightbooking;

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

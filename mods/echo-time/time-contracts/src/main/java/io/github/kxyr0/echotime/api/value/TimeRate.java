package io.github.kxyr0.echotime.api.value;

import java.math.BigDecimal;
import java.math.BigInteger;

public record TimeRate(long gameMicroseconds, long simulationTicks) {
    private static final BigInteger MICROS_PER_MINUTE = BigInteger.valueOf(TimeOfDay.MICROS_PER_MINUTE);
    private static final BigInteger TICKS_PER_SECOND = BigInteger.valueOf(20L);

    public TimeRate {
        if (gameMicroseconds < 0L || simulationTicks <= 0L) {
            throw new IllegalArgumentException("Time rate requires non-negative game time and positive ticks.");
        }
        long gcd = gcd(gameMicroseconds, simulationTicks);
        gameMicroseconds /= gcd;
        simulationTicks /= gcd;
    }

    public static TimeRate compile(BigDecimal gameMinutes, BigDecimal realSeconds) {
        if (gameMinutes.signum() < 0 || realSeconds.signum() <= 0) {
            throw new IllegalArgumentException("gameMinutes must be >= 0 and realSeconds must be > 0.");
        }
        Fraction game = fraction(gameMinutes);
        Fraction real = fraction(realSeconds);
        BigInteger numerator = game.numerator().multiply(MICROS_PER_MINUTE).multiply(real.denominator());
        BigInteger denominator = game.denominator().multiply(real.numerator()).multiply(TICKS_PER_SECOND);
        BigInteger gcd = numerator.gcd(denominator);
        return new TimeRate(numerator.divide(gcd).longValueExact(), denominator.divide(gcd).longValueExact());
    }

    public boolean stopped() {
        return gameMicroseconds == 0L;
    }

    private static Fraction fraction(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        int scale = Math.max(0, normalized.scale());
        return new Fraction(normalized.unscaledValue(), BigInteger.TEN.pow(scale));
    }

    private static long gcd(long left, long right) {
        long a = Math.abs(left);
        long b = Math.abs(right);
        while (b != 0L) {
            long next = a % b;
            a = b;
            b = next;
        }
        return Math.max(1L, a);
    }

    private record Fraction(BigInteger numerator, BigInteger denominator) { }
}

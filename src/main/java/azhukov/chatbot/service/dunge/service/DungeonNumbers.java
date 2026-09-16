package azhukov.chatbot.service.dunge.service;

import lombok.experimental.UtilityClass;

import java.math.BigInteger;

@UtilityClass
public class DungeonNumbers {
    public long add(long left, long right) {
        BigInteger value = BigInteger.valueOf(left).add(BigInteger.valueOf(right));
        return clamp(value);
    }

    public long multiply(long left, long right) {
        return clamp(BigInteger.valueOf(left).multiply(BigInteger.valueOf(right)));
    }

    public long multiplyDivide(long left, long right, long divisor) {
        if (divisor <= 0) throw new IllegalArgumentException("divisor must be positive");
        return clamp(BigInteger.valueOf(left).multiply(BigInteger.valueOf(right)).divide(BigInteger.valueOf(divisor)));
    }

    public long multiplyDivide(long first, long second, long third, long divisor) {
        if (divisor <= 0) throw new IllegalArgumentException("divisor must be positive");
        return clamp(BigInteger.valueOf(first).multiply(BigInteger.valueOf(second))
                .multiply(BigInteger.valueOf(third)).divide(BigInteger.valueOf(divisor)));
    }

    public long ceilMultiplyDivide(long value, long multiplier, long divisor) {
        if (divisor <= 0) throw new IllegalArgumentException("divisor must be positive");
        if (value <= 0 || multiplier <= 0) return 0L;
        BigInteger numerator = BigInteger.valueOf(value).multiply(BigInteger.valueOf(multiplier));
        BigInteger denominator = BigInteger.valueOf(divisor);
        return clamp(numerator.add(denominator).subtract(BigInteger.ONE).divide(denominator));
    }

    private long clamp(BigInteger value) {
        if (value.signum() < 0) return 0L;
        if (value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) return Long.MAX_VALUE;
        return value.longValue();
    }
}

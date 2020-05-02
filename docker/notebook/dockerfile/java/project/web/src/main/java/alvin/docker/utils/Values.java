package alvin.docker.utils;

import lombok.val;
import org.apache.logging.log4j.util.Strings;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Values {

    private Values() {
    }

    public static boolean truly(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    public static boolean truly(String s) {
        return Strings.isNotEmpty(s);
    }

    public static boolean truly(Number s) {
        return s != null && s.intValue() > 0;
    }

    public static boolean falsely(Boolean value) {
        return !truly(value);
    }

    public static boolean falsely(String value) {
        return !truly(value);
    }

    public static boolean falsely(Number value) {
        return !truly(value);
    }

    public static <T> T nullElse(T val, T elseValue) {
        return val == null ? elseValue : val;
    }

    public static <T> T nullElse(T val, Supplier<? extends T> orElse) {
        return val == null ? orElse.get() : val;
    }

    public static <T, R> R fetchIfNonNull(T value, Function<? super T, ? extends R> fetch) {
        return value == null ? null : fetch.apply(value);
    }

    public static <T, R> R fetchIfNonNull(T value, R def, Function<? super T, ? extends R> fetch) {
        return value == null ? def : fetch.apply(value);
    }

    public static <T, R> R fetchIfNonNull(T value, Supplier<? extends R> def, Function<? super T, ? extends R> fetch) {
        return value == null ? def.get() : fetch.apply(value);
    }

    public static <T> T choose(boolean condition, Supplier<? extends T> ifTrue, Supplier<? extends T> otherWish) {
        return condition ? ifTrue.get() : otherWish.get();
    }

    public static <T> T choose(boolean condition, T trueValue, Supplier<? extends T> otherWish) {
        return condition ? trueValue : otherWish.get();
    }

    public static <T> T choose(boolean condition, Supplier<? extends T> ifTrue, T otherValue) {
        return condition ? ifTrue.get() : otherValue;
    }

    public static <T, R> T choose(Supplier<? extends R> query, Function<? super R, Boolean> condition,
                                  Function<? super R, ? extends T> ifTrue,
                                  Function<? super R, ? extends T> otherWish) {
        val value = query.get();
        return condition.apply(value) ? ifTrue.apply(value) : otherWish.apply(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T forceConvert(Object variables) {
        return (T) variables;
    }

    @SafeVarargs
    public static <T> boolean in(T value, T... values) {
        for (T v : values) {
            if (Objects.equals(value, v)) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    public static <T> boolean notIn(T value, T... values) {
        return !in(value, values);
    }

    public static boolean isZero(Long num) {
        return num != null && num == 0;
    }
}

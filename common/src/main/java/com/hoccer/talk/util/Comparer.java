package com.hoccer.talk.util;

/*
 * Utility class provides a null-safe comparison method.
 * It uses the logic equal operator for primitive types and equals() for complex types.
 */
public class Comparer {

    public static <T> boolean isEqual(final T first, final T second) {
        if(first == null) {
            if(second == null) {
                return true;
            } else {
                return false;
            }
        } else {
            if(second == null) {
                return false;
            } else {
                return first.equals(second);
            }
        }
    }

    public static boolean isEqual(final byte first, final byte second) {
        return first == second;
    }

    public static boolean isEqual(final short first, final short second) {
        return first == second;
    }

    public static boolean isEqual(final int first, final int second) {
        return first == second;
    }

    public static boolean isEqual(final long first, final long second) {
        return first == second;
    }

    public static boolean isEqual(final float first, final float second) {
        return first == second;
    }

    public static boolean isEqual(final double first, final double second) {
        return first == second;
    }

    public static boolean isEqual(final boolean first, final boolean second) {
        return first == second;
    }

    public static boolean isEqual(final char first, final char second) {
        return first == second;
    }
}

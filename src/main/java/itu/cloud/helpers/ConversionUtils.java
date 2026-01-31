package itu.cloud.helpers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public final class ConversionUtils {

    private ConversionUtils() {}

    public static BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;

        if (o instanceof Number) {
            double d = ((Number) o).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return BigDecimal.valueOf(d);
        }

        if (o instanceof String) {
            String s = ((String) o).trim();
            if (s.isEmpty()) return null;
            if ("NaN".equalsIgnoreCase(s) || "Infinity".equalsIgnoreCase(s) || "-Infinity".equalsIgnoreCase(s))
                return null;
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                try {
                    double d = Double.parseDouble(s);
                    if (Double.isNaN(d) || Double.isInfinite(d)) return null;
                    return BigDecimal.valueOf(d);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }

        return null;
    }

    public static Integer toInteger(Object o) {
        if (o == null) return null;

        if (o instanceof Number) {
            double d = ((Number) o).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return ((Number) o).intValue();
        }

        if (o instanceof String) {
            String s = ((String) o).trim();
            if (s.isEmpty()) return null;
            if ("NaN".equalsIgnoreCase(s) || "Infinity".equalsIgnoreCase(s) || "-Infinity".equalsIgnoreCase(s))
                return null;
            try {
                double d = Double.parseDouble(s);
                if (Double.isNaN(d) || Double.isInfinite(d)) return null;
                return (int) d;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    public static Instant toInstant(Object o) {
        if (o == null) return null;
        if (o instanceof Instant) return (Instant) o;
        try {
            return Instant.parse(o.toString());
        } catch (DateTimeParseException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

package com.rescuelink.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Converts between Java date-times and the text format stored in SQLite. */
public final class DateTimeUtil {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {
    }

    public static String nowString() {
        return LocalDateTime.now().format(FORMAT);
    }

    public static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text, FORMAT);
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime.format(FORMAT);
    }
}

package org.example.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    public static String getCurrentDate() {
        return LocalDate.now().toString(); // YYYY-MM-DD
    }

    public static String getCurrentDateTime() {
        var now = java.time.LocalDateTime.now();
        return "%04d-%02d-%02d %02d:%02d:%02d".formatted(
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute(), now.getSecond()
        );
    }

    public static boolean isBefore(String date1, String date2) {
        return date1.compareTo(date2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        return date1.compareTo(date2) > 0;
    }

    public static String addDays(String date, int days) {
        return LocalDate.parse(date).plusDays(days).toString();
    }

    public static String formatRelativeTime(String date) {
        String today = getCurrentDate();
        long days = ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(date));
        if (days == 0) return "today";
        if (days < 0) return Math.abs(days) + " days ago";
        return "in " + days + " days";
    }
}
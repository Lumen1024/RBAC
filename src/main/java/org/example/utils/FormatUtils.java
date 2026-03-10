package org.example.utils;

import java.util.List;

public class FormatUtils {

    private static final int MAX_COL_WIDTH = 40;

    public static String formatTable(String title, String[] headers, List<String[]> rows) {
        return formatHeader(title) + "\n" + formatTable(headers, rows);
    }

    public static String formatTable(String[] headers, List<String[]> rows) {
        int cols = headers.length;
        int[] widths = new int[cols];

        for (int i = 0; i < cols; i++)
            widths[i] = headers[i].length();

        for (var row : rows)
            for (int i = 0; i < cols && i < row.length; i++) {
                String val = row[i] == null ? "" : truncate(row[i], MAX_COL_WIDTH);
                widths[i] = Math.max(widths[i], val.length());
            }

        StringBuilder sb = new StringBuilder();
        String separator = buildSeparator(widths);

        sb.append(separator).append("\n");
        sb.append(buildRow(headers, widths)).append("\n");
        sb.append(separator).append("\n");
        for (String[] row : rows)
            sb.append(buildRow(row, widths)).append("\n");

        sb.append(separator);

        return sb.toString();
    }

    private static String buildSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        return sb.toString();
    }

    private static String buildRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String cell = (i < cells.length && cells[i] != null) ? truncate(cells[i], MAX_COL_WIDTH) : "";
            sb.append(" ").append(padRight(cell, widths[i])).append(" |");
        }
        return sb.toString();
    }

    public static String formatBox(String text) {
        String[] lines = text.split("\n");
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, line.length());
        }


        String border = "+" + "-".repeat(width + 2) + "+";
        StringBuilder sb = new StringBuilder();
        sb.append(border).append("\n");
        for (String line : lines) {
            sb.append("| ").append(padRight(line, width)).append(" |\n");
        }
        sb.append(border);
        return sb.toString();
    }

    public static String formatHeader(String text) {
        int width = text.length() + 4;
        String line = "=".repeat(width);
        return line + "\n" +
                "= " + text + " =" + "\n" +
                line;
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (maxLength <= 3) return text.substring(0, Math.min(maxLength, text.length()));
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        return text + " ".repeat(length - text.length());
    }

    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        return " ".repeat(length - text.length()) + text;
    }
}
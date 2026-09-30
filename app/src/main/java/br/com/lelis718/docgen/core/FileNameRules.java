package br.com.lelis718.docgen.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class FileNameRules {

    public static final String PDF_EXTENSION = ".pdf";
    private static final int MAX_BASE_LENGTH = 80;
    private static final String DEFAULT_PATTERN = "yyyyMMdd_HHmmss";
    private static final String ILLEGAL = "\\/:*?\"<>|";

    private FileNameRules() {
    }

    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < 32 || c == 127 || ILLEGAL.indexOf(c) >= 0) {
                builder.append(' ');
            } else {
                builder.append(c);
            }
        }
        String cleaned = builder.toString().trim();
        while (cleaned.contains("  ")) {
            cleaned = cleaned.replace("  ", " ");
        }
        while (cleaned.startsWith(".") || cleaned.startsWith(" ")) {
            cleaned = cleaned.substring(1).trim();
        }
        while (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        if (cleaned.isEmpty()) {
            return "";
        }
        String base = cleaned;
        String extension = "";
        int dot = cleaned.lastIndexOf('.');
        if (dot > 0 && cleaned.length() - dot <= 11) {
            base = cleaned.substring(0, dot);
            extension = cleaned.substring(dot);
        }
        if (base.length() > MAX_BASE_LENGTH) {
            base = base.substring(0, MAX_BASE_LENGTH).trim();
        }
        if (base.isEmpty()) {
            return "";
        }
        return base + extension;
    }

    public static String ensurePdfExtension(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(PDF_EXTENSION)
                ? name
                : name + PDF_EXTENSION;
    }

    public static String defaultName() {
        return defaultName(LocalDateTime.now());
    }

    public static String defaultName(LocalDateTime moment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);
        return ensurePdfExtension("DocGen_" + moment.format(formatter));
    }

    public static String withoutExtension(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return name;
        }
        if (dot == 0) {
            return "";
        }
        return name.substring(0, dot);
    }
}

package br.com.lelis718.docgen.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ShareParser {

    private ShareParser() {
    }

    public static List<SharedItem> parse(List<String> uriOrder, String intentMimeType) {
        String mime = normalizeMime(intentMimeType);
        List<SharedItem> items = new ArrayList<>();
        if (uriOrder != null) {
            for (String uri : uriOrder) {
                if (uri != null && !uri.trim().isEmpty()) {
                    items.add(SharedItem.forUri(uri.trim(), mime));
                }
            }
        }
        return Collections.unmodifiableList(items);
    }

    public static SharedItem text(String content, String intentMimeType) {
        return SharedItem.forText(content, normalizeMime(intentMimeType));
    }

    private static String normalizeMime(String mime) {
        if (mime == null) {
            return null;
        }
        String normalized = mime.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.equals("*/*") || normalized.equals("application/octet-stream")) {
            return null;
        }
        return normalized;
    }
}

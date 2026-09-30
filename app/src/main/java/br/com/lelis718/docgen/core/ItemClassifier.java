package br.com.lelis718.docgen.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ItemClassifier {

    private static final Map<String, ItemKind> BY_EXTENSION = new HashMap<>();

    static {
        for (String ext : new String[]{"png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif",
                "avif", "tif", "tiff", "dng"}) {
            BY_EXTENSION.put(ext, ItemKind.IMAGE);
        }
        BY_EXTENSION.put("pdf", ItemKind.PDF);
        for (String ext : new String[]{"txt", "log", "csv", "md", "json", "xml", "ini", "conf",
                "properties", "yml", "yaml", "java", "gradle"}) {
            BY_EXTENSION.put(ext, ItemKind.TEXT);
        }
    }

    private ItemClassifier() {
    }

    public static ItemKind classify(String mimeType, String uri) {
        ItemKind byMime = fromMime(mimeType);
        if (byMime != null) {
            return byMime;
        }
        return fromExtension(uri);
    }

    private static ItemKind fromMime(String mime) {
        if (mime == null) {
            return null;
        }
        String normalized = mime.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.equals("*/*")
                || normalized.equals("application/octet-stream")) {
            return null;
        }
        if (normalized.startsWith("image/")) {
            return ItemKind.IMAGE;
        }
        if (normalized.equals("application/pdf")) {
            return ItemKind.PDF;
        }
        if (normalized.startsWith("text/")) {
            return ItemKind.TEXT;
        }
        if (normalized.endsWith("+xml") || normalized.endsWith("+json")
                || normalized.equals("application/json") || normalized.equals("application/xml")) {
            return ItemKind.TEXT;
        }
        return ItemKind.UNSUPPORTED;
    }

    private static ItemKind fromExtension(String uri) {
        if (uri == null) {
            return ItemKind.UNSUPPORTED;
        }
        String path = uri;
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int fragment = path.indexOf('#');
        if (fragment >= 0) {
            path = path.substring(0, fragment);
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (slash >= 0) {
            path = path.substring(slash + 1);
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return ItemKind.UNSUPPORTED;
        }
        ItemKind kind = BY_EXTENSION.get(path.substring(dot + 1).toLowerCase(Locale.ROOT));
        return kind == null ? ItemKind.UNSUPPORTED : kind;
    }
}

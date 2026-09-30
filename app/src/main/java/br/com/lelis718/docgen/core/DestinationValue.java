package br.com.lelis718.docgen.core;

public final class DestinationValue {

    public static final String DOWNLOADS = "downloads";
    public static final String DOCUMENTS = "documents";
    private static final String TREE_PREFIX = "tree:";

    private DestinationValue() {
    }

    public static String tree(String treeUri) {
        return TREE_PREFIX + treeUri;
    }

    public static boolean isTree(String value) {
        return value != null && value.startsWith(TREE_PREFIX);
    }

    public static boolean isDocuments(String value) {
        return DOCUMENTS.equals(value);
    }

    public static boolean isDownloads(String value) {
        return value == null || DOWNLOADS.equals(value);
    }

    public static String uriOf(String value) {
        return isTree(value) ? value.substring(TREE_PREFIX.length()) : "";
    }
}

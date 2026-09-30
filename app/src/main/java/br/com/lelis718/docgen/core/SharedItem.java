package br.com.lelis718.docgen.core;

public final class SharedItem {

    private final String uri;
    private final String inlineText;
    private final String mimeType;

    private SharedItem(String uri, String inlineText, String mimeType) {
        this.uri = uri;
        this.inlineText = inlineText;
        this.mimeType = mimeType;
    }

    public static SharedItem forUri(String uri, String mimeType) {
        return new SharedItem(uri, null, mimeType);
    }

    public static SharedItem forText(String text, String mimeType) {
        return new SharedItem(null, text, mimeType);
    }

    public String uri() {
        return uri;
    }

    public String inlineText() {
        return inlineText;
    }

    public String mimeType() {
        return mimeType;
    }

    public boolean hasUri() {
        return uri != null;
    }
}

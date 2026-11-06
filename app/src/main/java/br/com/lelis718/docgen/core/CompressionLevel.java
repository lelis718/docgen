package br.com.lelis718.docgen.core;

public enum CompressionLevel {
    ALTA(1.0f),
    MEDIA(1.5f),
    BAIXA(2.0f);

    private static final CompressionLevel DEFAULT = MEDIA;

    private final float renderScale;

    CompressionLevel(float renderScale) {
        this.renderScale = renderScale;
    }

    public float renderScale() {
        return renderScale;
    }

    public static CompressionLevel fromIndex(int index) {
        CompressionLevel[] levels = values();
        if (index < 0 || index >= levels.length) {
            return DEFAULT;
        }
        return levels[index];
    }

    public static CompressionLevel defaultLevel() {
        return DEFAULT;
    }
}

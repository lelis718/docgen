package br.com.lelis718.docgen.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Element {

    public enum Type {
        IMAGE,
        PDF_PAGE,
        TEXT
    }

    private final Type type;
    private final int itemIndex;
    private final int sourcePageIndex;
    private final Box src;
    private final Box dest;
    private final List<String> lines;
    private final float fontSize;
    private final float lineHeight;

    private Element(Type type, int itemIndex, int sourcePageIndex, Box src, Box dest,
                    List<String> lines, float fontSize, float lineHeight) {
        this.type = type;
        this.itemIndex = itemIndex;
        this.sourcePageIndex = sourcePageIndex;
        this.src = src;
        this.dest = dest;
        this.lines = Collections.unmodifiableList(new ArrayList<>(
                lines == null ? Collections.<String>emptyList() : lines));
        this.fontSize = fontSize;
        this.lineHeight = lineHeight;
    }

    public static Element image(int itemIndex, Box src, Box dest) {
        return new Element(Type.IMAGE, itemIndex, -1, src, dest, null, 0f, 0f);
    }

    public static Element pdfPage(int itemIndex, int sourcePageIndex, Box dest) {
        return new Element(Type.PDF_PAGE, itemIndex, sourcePageIndex, null, dest, null, 0f, 0f);
    }

    public static Element text(int itemIndex, List<String> lines, Box dest, float fontSize,
                               float lineHeight) {
        return new Element(Type.TEXT, itemIndex, -1, null, dest, lines, fontSize, lineHeight);
    }

    public Type type() {
        return type;
    }

    public int itemIndex() {
        return itemIndex;
    }

    public int sourcePageIndex() {
        return sourcePageIndex;
    }

    public Box src() {
        return src;
    }

    public Box dest() {
        return dest;
    }

    public List<String> lines() {
        return lines;
    }

    public float fontSize() {
        return fontSize;
    }

    public float lineHeight() {
        return lineHeight;
    }
}

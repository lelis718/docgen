package br.com.lelis718.docgen.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SourceDescriptor {

    public enum Kind {
        IMAGE,
        PDF,
        TEXT
    }

    private final String uri;
    private final String name;
    private final Kind kind;
    private final float width;
    private final float height;
    private final List<Box> pages;
    private final String text;

    private SourceDescriptor(String uri, String name, Kind kind, float width, float height,
                             List<Box> pages, String text) {
        this.uri = uri;
        this.name = name;
        this.kind = kind;
        this.width = width;
        this.height = height;
        this.pages = pages == null
                ? Collections.<Box>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(pages));
        this.text = text;
    }

    public static SourceDescriptor image(String uri, String name, float width, float height) {
        return new SourceDescriptor(uri, name, Kind.IMAGE, width, height, null, null);
    }

    public static SourceDescriptor pdf(String uri, String name, List<Box> pageSizes) {
        return new SourceDescriptor(uri, name, Kind.PDF, 0f, 0f, pageSizes, null);
    }

    public static SourceDescriptor text(String uri, String name, String content) {
        return new SourceDescriptor(uri, name, Kind.TEXT, 0f, 0f, null, content);
    }

    public String uri() {
        return uri;
    }

    public String name() {
        return name;
    }

    public Kind kind() {
        return kind;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public int pageCount() {
        return pages.size();
    }

    public Box pageSize(int index) {
        return pages.get(index);
    }

    public List<Box> pages() {
        return pages;
    }

    public String text() {
        return text;
    }
}

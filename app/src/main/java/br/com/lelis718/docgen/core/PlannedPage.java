package br.com.lelis718.docgen.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlannedPage {

    private final int index;
    private final float width;
    private final float height;
    private final List<Element> elements = new ArrayList<>();

    public PlannedPage(int index, float width, float height) {
        this.index = index;
        this.width = width;
        this.height = height;
    }

    public int index() {
        return index;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    void add(Element element) {
        elements.add(element);
    }

    public List<Element> elements() {
        return Collections.unmodifiableList(elements);
    }
}

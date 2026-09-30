package br.com.lelis718.docgen.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Resolution {

    private final List<SourceDescriptor> items;
    private final List<String> skippedNames;

    public Resolution(List<SourceDescriptor> items, List<String> skippedNames) {
        this.items = Collections.unmodifiableList(new ArrayList<>(
                items == null ? Collections.<SourceDescriptor>emptyList() : items));
        this.skippedNames = Collections.unmodifiableList(new ArrayList<>(
                skippedNames == null ? Collections.<String>emptyList() : skippedNames));
    }

    public List<SourceDescriptor> items() {
        return items;
    }

    public List<String> skippedNames() {
        return skippedNames;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}

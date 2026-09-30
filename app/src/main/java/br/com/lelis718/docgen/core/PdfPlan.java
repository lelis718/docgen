package br.com.lelis718.docgen.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PdfPlan {

    private final List<PlannedPage> pages;

    public PdfPlan(List<PlannedPage> pages) {
        this.pages = Collections.unmodifiableList(new ArrayList<>(
                pages == null ? Collections.<PlannedPage>emptyList() : pages));
    }

    public List<PlannedPage> pages() {
        return pages;
    }

    public int pageCount() {
        return pages.size();
    }

    public int elementCount() {
        int total = 0;
        for (PlannedPage page : pages) {
            total += page.elements().size();
        }
        return total;
    }

    public boolean isEmpty() {
        return pages.isEmpty();
    }
}

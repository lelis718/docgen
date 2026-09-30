package br.com.lelis718.docgen.core;

import java.util.ArrayList;
import java.util.List;

public final class PdfLayoutPlanner {

    public static final float TEXT_FONT_SIZE = 11f;
    public static final float TEXT_LINE_HEIGHT = TEXT_FONT_SIZE * 1.35f;
    public static final float IMAGE_GAP = 10f;

    private final TextTypesetter.LineMeasurer measurer;

    public PdfLayoutPlanner(TextTypesetter.LineMeasurer measurer) {
        if (measurer == null) {
            throw new IllegalArgumentException("measurer == null");
        }
        this.measurer = measurer;
    }

    public PdfPlan plan(List<SourceDescriptor> items, PageGeometry geometry) {
        List<PlannedPage> pages = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return new PdfPlan(pages);
        }
        Box content = geometry.contentBox();
        float cursorY = content.top;
        boolean canAppend = false;

        for (int index = 0; index < items.size(); index++) {
            SourceDescriptor item = items.get(index);
            switch (item.kind()) {
                case IMAGE:
                    Box fitted = ImageFit.topAligned(item.width(), item.height(),
                            content.width(), content.height(), content.left, content.top);
                    if (!canAppend || cursorY + fitted.height() > content.bottom + 0.5f) {
                        pages.add(new PlannedPage(pages.size() + 1, geometry.width(), geometry.height()));
                        cursorY = content.top;
                    }
                    Box dest = Box.of(content.left, cursorY, fitted.width(), fitted.height());
                    pages.get(pages.size() - 1).add(Element.image(index, srcBox(item), dest));
                    cursorY = dest.bottom + IMAGE_GAP;
                    canAppend = true;
                    break;

                case PDF:
                    for (int pageIndex = 0; pageIndex < item.pageCount(); pageIndex++) {
                        Box pageSize = item.pageSize(pageIndex);
                        Box pageDest = ImageFit.contain(pageSize.width(), pageSize.height(), content);
                        PlannedPage page = new PlannedPage(pages.size() + 1, geometry.width(), geometry.height());
                        page.add(Element.pdfPage(index, pageIndex, pageDest));
                        pages.add(page);
                    }
                    canAppend = false;
                    cursorY = content.bottom;
                    break;

                case TEXT:
                    List<String> lines = TextTypesetter.wrap(item.text(), content.width(), measurer);
                    int perPage = Math.max(1, (int) (content.height() / TEXT_LINE_HEIGHT));
                    int lineIndex = 0;
                    while (lineIndex < lines.size()) {
                        int count = Math.min(perPage, lines.size() - lineIndex);
                        List<String> pageLines = new ArrayList<>(lines.subList(lineIndex, lineIndex + count));
                        Box textDest = Box.of(content.left, content.top, content.width(),
                                count * TEXT_LINE_HEIGHT);
                        PlannedPage page = new PlannedPage(pages.size() + 1, geometry.width(), geometry.height());
                        page.add(Element.text(index, pageLines, textDest, TEXT_FONT_SIZE, TEXT_LINE_HEIGHT));
                        pages.add(page);
                        lineIndex += count;
                    }
                    canAppend = false;
                    cursorY = content.bottom;
                    break;

                default:
                    break;
            }
        }
        return new PdfPlan(pages);
    }

    private static Box srcBox(SourceDescriptor item) {
        return Box.of(0f, 0f, item.width(), item.height());
    }
}

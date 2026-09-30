package br.com.lelis718.docgen.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PdfLayoutPlannerTest {

    private static final PageGeometry A4 = PageGeometry.A4;
    private static final float CONTENT_LEFT = 36f;
    private static final float CONTENT_TOP = 36f;
    private static final float CONTENT_RIGHT = 559f;
    private static final float CONTENT_BOTTOM = 806f;
    private static final TextTypesetter.LineMeasurer FIVE_PER_CHAR =
            line -> line.length() * 5f;

    private final PdfLayoutPlanner planner = new PdfLayoutPlanner(FIVE_PER_CHAR);

    @Test
    public void listaVaziaNaoGeraPaginas() {
        PdfPlan plan = planner.plan(Collections.<SourceDescriptor>emptyList(), A4);

        assertEquals(0, plan.pageCount());
        assertTrue(plan.isEmpty());
    }

    @Test
    public void imagemUnicaOcupaUmaPaginaNaMargem() {
        PdfPlan plan = planner.plan(
                Collections.singletonList(SourceDescriptor.image("u", "a.png", 800f, 600f)), A4);

        assertEquals(1, plan.pageCount());
        Element element = plan.pages().get(0).elements().get(0);
        assertEquals(Element.Type.IMAGE, element.type());
        assertEquals(CONTENT_LEFT, element.dest().left, 0.01f);
        assertEquals(CONTENT_TOP, element.dest().top, 0.01f);
        assertEquals(523f, element.dest().width(), 0.01f);
        assertEquals(392.25f, element.dest().height(), 0.01f);
        assertEquals(800f, element.src().width(), 0.01f);
        assertEquals(600f, element.src().height(), 0.01f);
        assertTrue(element.dest().bottom <= CONTENT_BOTTOM);
    }

    @Test
    public void imagensPequenasEmpilhamUmaAbaixoDaOutra() {
        List<SourceDescriptor> items = Arrays.asList(
                SourceDescriptor.image("u1", "p1.png", 2000f, 300f),
                SourceDescriptor.image("u2", "p2.png", 2000f, 300f),
                SourceDescriptor.image("u3", "p3.png", 2000f, 300f));

        PdfPlan plan = planner.plan(items, A4);

        assertEquals(1, plan.pageCount());
        List<Element> elements = plan.pages().get(0).elements();
        assertEquals(3, elements.size());

        float previousBottom = 0f;
        for (Element element : elements) {
            assertEquals(Element.Type.IMAGE, element.type());
            assertTrue(element.dest().top > previousBottom);
            assertTrue(element.dest().top >= CONTENT_TOP);
            assertTrue(element.dest().bottom <= CONTENT_BOTTOM);
            previousBottom = element.dest().bottom;
        }
    }

    @Test
    public void imagemGrandeQueNaoCabeQuebraPagina() {
        List<SourceDescriptor> items = Arrays.asList(
                SourceDescriptor.image("u1", "a.png", 800f, 600f),
                SourceDescriptor.image("u2", "b.png", 800f, 600f),
                SourceDescriptor.image("u3", "c.png", 800f, 600f));

        PdfPlan plan = planner.plan(items, A4);

        assertEquals(3, plan.pageCount());
        for (PlannedPage page : plan.pages()) {
            assertEquals(1, page.elements().size());
            assertEquals(CONTENT_TOP, page.elements().get(0).dest().top, 0.01f);
        }
    }

    @Test
    public void pdfGeraUmaSaidaPorPaginaDeOrigem() {
        SourceDescriptor pdf = SourceDescriptor.pdf("u", "relatorio.pdf",
                Arrays.asList(Box.of(0f, 0f, 595f, 842f),
                        Box.of(0f, 0f, 595f, 842f),
                        Box.of(0f, 0f, 100f, 100f)));

        PdfPlan plan = planner.plan(Collections.singletonList(pdf), A4);

        assertEquals(3, plan.pageCount());
        assertEquals(3, plan.elementCount());
        for (int i = 0; i < 3; i++) {
            Element element = plan.pages().get(i).elements().get(0);
            assertEquals(Element.Type.PDF_PAGE, element.type());
            assertEquals(i, element.sourcePageIndex());
            assertEquals(0, element.itemIndex());
            assertTrue(element.dest().left >= CONTENT_LEFT);
            assertTrue(element.dest().right <= CONTENT_RIGHT);
            assertTrue(element.dest().top >= CONTENT_TOP);
            assertTrue(element.dest().bottom <= CONTENT_BOTTOM);
        }
    }

    @Test
    public void paginasDeOrigemComTamanhosDiferentesSaoCentralizadas() {
        SourceDescriptor pdf = SourceDescriptor.pdf("u", "retrato.pdf",
                Collections.singletonList(Box.of(0f, 0f, 200f, 100f)));

        PdfPlan plan = planner.plan(Collections.singletonList(pdf), A4);

        Element element = plan.pages().get(0).elements().get(0);
        assertEquals(523f, element.dest().width(), 0.01f);
        assertEquals(261.5f, element.dest().height(), 0.01f);
        assertEquals(CONTENT_TOP + (770f - 261.5f) / 2f, element.dest().top, 0.01f);
        assertTrue(element.dest().bottom <= CONTENT_BOTTOM);
    }

    @Test
    public void selecaoMistaIntercaleNaOrdemOriginal() {
        List<SourceDescriptor> items = Arrays.asList(
                SourceDescriptor.image("u1", "foto.png", 800f, 600f),
                SourceDescriptor.pdf("u2", "doc.pdf", Arrays.asList(
                        Box.of(0f, 0f, 595f, 842f),
                        Box.of(0f, 0f, 595f, 842f))),
                SourceDescriptor.image("u3", "panorama.png", 2000f, 300f));

        PdfPlan plan = planner.plan(items, A4);

        assertEquals(4, plan.pageCount());
        assertEquals(Element.Type.IMAGE, plan.pages().get(0).elements().get(0).type());
        assertEquals(0, plan.pages().get(0).elements().get(0).itemIndex());

        Element firstPdfPage = plan.pages().get(1).elements().get(0);
        assertEquals(Element.Type.PDF_PAGE, firstPdfPage.type());
        assertEquals(1, firstPdfPage.itemIndex());
        assertEquals(0, firstPdfPage.sourcePageIndex());
        assertEquals(1, plan.pages().get(2).elements().get(0).sourcePageIndex());

        Element imageAfterPdf = plan.pages().get(3).elements().get(0);
        assertEquals(Element.Type.IMAGE, imageAfterPdf.type());
        assertEquals(2, imageAfterPdf.itemIndex());
        assertEquals(CONTENT_TOP, imageAfterPdf.dest().top, 0.01f);
    }

    @Test
    public void textoComecaEmPaginaNovaMesmoComEspacoLivre() {
        List<SourceDescriptor> items = Arrays.asList(
                SourceDescriptor.image("u1", "foto.png", 800f, 600f),
                SourceDescriptor.text("u2", "notas.txt", "primeira linha"));

        PdfPlan plan = planner.plan(items, A4);

        assertEquals(2, plan.pageCount());
        assertEquals(Element.Type.IMAGE, plan.pages().get(0).elements().get(0).type());
        Element text = plan.pages().get(1).elements().get(0);
        assertEquals(Element.Type.TEXT, text.type());
        assertEquals(CONTENT_TOP, text.dest().top, 0.01f);
        assertEquals(1, text.lines().size());
        assertEquals(PdfLayoutPlanner.TEXT_LINE_HEIGHT, text.lineHeight(), 0.001f);
        assertEquals(PdfLayoutPlanner.TEXT_FONT_SIZE, text.fontSize(), 0.001f);
    }

    @Test
    public void textoLongoEhPaginado() {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 120; i++) {
            if (i > 0) {
                content.append('\n');
            }
            content.append("linha ").append(String.format("%03d", i));
        }

        PdfPlan plan = planner.plan(
                Collections.singletonList(SourceDescriptor.text("u", "grande.txt", content.toString())),
                A4);

        assertEquals(3, plan.pageCount());
        assertEquals(51, plan.pages().get(0).elements().get(0).lines().size());
        assertEquals(51, plan.pages().get(1).elements().get(0).lines().size());
        assertEquals(18, plan.pages().get(2).elements().get(0).lines().size());
        assertEquals("linha 000", plan.pages().get(0).elements().get(0).lines().get(0));
        assertEquals("linha 119", plan.pages().get(2).elements().get(0).lines().get(17));
        for (PlannedPage page : plan.pages()) {
            Element element = page.elements().get(0);
            assertEquals(523f, element.dest().width(), 0.01f);
            assertTrue(element.dest().bottom <= CONTENT_BOTTOM + 0.01f);
        }
    }

    @Test
    public void textoComLinhasLongasEhQuebrado() {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            line.append("palavra ");
        }

        PdfPlan plan = planner.plan(
                Collections.singletonList(SourceDescriptor.text("u", "t.txt", line.toString())), A4);

        assertEquals(1, plan.pageCount());
        List<String> lines = plan.pages().get(0).elements().get(0).lines();
        assertTrue(lines.size() > 1);
        for (String text : lines) {
            assertTrue(text.length() <= 104);
        }
    }

    @Test
    public void itensNaoSuportadosNaoAparecemNoPlano() {
        List<SourceDescriptor> items = new ArrayList<>();
        items.add(SourceDescriptor.image("u1", "a.png", 800f, 600f));

        PdfPlan plan = planner.plan(items, A4);

        assertEquals(1, plan.elementCount());
        assertEquals(1, plan.pageCount());
    }
}

package br.com.lelis718.docgen.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShareParserTest {

    @Test
    public void preservaOrdemDeSelecao() {
        List<SharedItem> items = ShareParser.parse(
                Arrays.asList("content://a/3.png", "content://a/1.png", "content://a/2.pdf"),
                "image/*");

        assertEquals(3, items.size());
        assertEquals("content://a/3.png", items.get(0).uri());
        assertEquals("content://a/1.png", items.get(1).uri());
        assertEquals("content://a/2.pdf", items.get(2).uri());
    }

    @Test
    public void ignoraUrisVaziasENulas() {
        List<SharedItem> items = ShareParser.parse(
                Arrays.asList(null, "  ", "content://a/unico.jpg"), "*/*");

        assertEquals(1, items.size());
        assertEquals("content://a/unico.jpg", items.get(0).uri());
    }

    @Test
    public void listaNulaResultaEmVazio() {
        assertTrue(ShareParser.parse(null, "image/png").isEmpty());
        assertTrue(ShareParser.parse(Collections.<String>emptyList(), null).isEmpty());
    }

    @Test
    public void mimeGenericoViraNulo() {
        assertNull(ShareParser.parse(Collections.singletonList("content://a/x"), "*/*")
                .get(0).mimeType());
        assertNull(ShareParser.parse(
                Collections.singletonList("content://a/x"), "application/octet-stream")
                .get(0).mimeType());
    }

    @Test
    public void mimeEhNormalizado() {
        assertEquals("image/png", ShareParser.parse(
                Collections.singletonList("content://a/x"), " IMAGE/PNG ").get(0).mimeType());
    }

    @Test
    public void textoCompartilhadoVirarItem() {
        SharedItem item = ShareParser.text("  ola  ", "text/plain");

        assertTrue(!item.hasUri());
        assertEquals("  ola  ", item.inlineText());
        assertEquals("text/plain", item.mimeType());
    }
}

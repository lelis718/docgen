package br.com.lelis718.docgen.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ItemClassifierTest {

    @Test
    public void classificaPorMime() {
        assertEquals(ItemKind.IMAGE, ItemClassifier.classify("image/png", null));
        assertEquals(ItemKind.IMAGE, ItemClassifier.classify("image/heic", null));
        assertEquals(ItemKind.PDF, ItemClassifier.classify("application/pdf", null));
        assertEquals(ItemKind.TEXT, ItemClassifier.classify("text/plain", null));
        assertEquals(ItemKind.TEXT, ItemClassifier.classify("text/csv", null));
        assertEquals(ItemKind.TEXT, ItemClassifier.classify("application/json", null));
        assertEquals(ItemKind.TEXT, ItemClassifier.classify("application/atom+xml", null));
    }

    @Test
    public void mimeInutilCaiParaExtensao() {
        assertEquals(ItemKind.PDF,
                ItemClassifier.classify("application/octet-stream", "content://docs/relatorio.pdf"));
        assertEquals(ItemKind.IMAGE,
                ItemClassifier.classify(null, "content://images/foto.jpeg?size=large"));
        assertEquals(ItemKind.TEXT,
                ItemClassifier.classify("", "content://docs/notas.txt"));
        assertEquals(ItemKind.UNSUPPORTED,
                ItemClassifier.classify("*/*", "content://docs/plano.xyz"));
    }

    @Test
    public void mimeDesconhecidoSemExtensaoEhIgnorado() {
        assertEquals(ItemKind.UNSUPPORTED,
                ItemClassifier.classify("application/msword", "content://docs/modelo"));
        assertEquals(ItemKind.UNSUPPORTED, ItemClassifier.classify(null, null));
    }

    @Test
    public void mimeNormalizadoEhCaseInsensitive() {
        assertEquals(ItemKind.IMAGE, ItemClassifier.classify(" Image/PNG ", null));
        assertEquals(ItemKind.UNSUPPORTED,
                ItemClassifier.classify("application/zip", null));
    }

    @Test
    public void mimeGenericoUsaExtensao() {
        assertEquals(ItemKind.IMAGE,
                ItemClassifier.classify("*/*", "content://x/a.webp"));
        assertEquals(ItemKind.PDF,
                ItemClassifier.classify(null, "content://x/a.pdf"));
    }

    @Test
    public void extensaoComPontoFinalEhIgnorada() {
        assertEquals(ItemKind.UNSUPPORTED,
                ItemClassifier.classify(null, "content://docs/arquivo."));
        assertTrue(ItemClassifier.classify(null, "content://docs/a.pdf") == ItemKind.PDF);
    }
}

package br.com.lelis718.docgen.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CompressionLevelTest {

    @Test
    public void maiorCompressaoUsaMenorEscala() {
        assertTrue(CompressionLevel.ALTA.renderScale()
                < CompressionLevel.MEDIA.renderScale());
        assertTrue(CompressionLevel.MEDIA.renderScale()
                < CompressionLevel.BAIXA.renderScale());
    }

    @Test
    public void indiceMapeiaParaNivelCorrespondente() {
        assertEquals(CompressionLevel.ALTA, CompressionLevel.fromIndex(0));
        assertEquals(CompressionLevel.MEDIA, CompressionLevel.fromIndex(1));
        assertEquals(CompressionLevel.BAIXA, CompressionLevel.fromIndex(2));
    }

    @Test
    public void indiceInvalidoUsaNivelPadrao() {
        assertEquals(CompressionLevel.defaultLevel(), CompressionLevel.fromIndex(-1));
        assertEquals(CompressionLevel.defaultLevel(), CompressionLevel.fromIndex(99));
    }
}

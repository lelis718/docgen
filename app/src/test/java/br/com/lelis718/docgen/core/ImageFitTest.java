package br.com.lelis718.docgen.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ImageFitTest {

    private static final Box TARGET = new Box(36f, 36f, 559f, 806f);

    @Test
    public void paisagemPreencheLarguraECentraVertical() {
        Box result = ImageFit.contain(1000f, 500f, TARGET);

        assertEquals(523f, result.width(), 0.01f);
        assertEquals(261.5f, result.height(), 0.01f);
        assertEquals(36f, result.left, 0.01f);
        assertEquals(559f, result.right, 0.01f);
        assertEquals(TARGET.top + (TARGET.height() - result.height()) / 2f, result.top, 0.01f);
    }

    @Test
    public void retratoPreencheAlturaECentraHorizontal() {
        Box result = ImageFit.contain(500f, 1000f, TARGET);

        assertEquals(770f, result.height(), 0.01f);
        assertEquals(385f, result.width(), 0.01f);
        assertEquals(TARGET.left + (TARGET.width() - result.width()) / 2f, result.left, 0.01f);
        assertEquals(TARGET.top, result.top, 0.01f);
        assertEquals(TARGET.bottom, result.bottom, 0.01f);
    }

    @Test
    public void quadradoUsaMenorEscala() {
        Box result = ImageFit.contain(400f, 400f, TARGET);

        assertEquals(400f * (523f / 400f), result.width(), 0.01f);
        assertEquals(result.width(), result.height(), 0.01f);
    }

    @Test
    public void dimensoesInvalidasResultamEmCaixaVazia() {
        Box result = ImageFit.contain(0f, 100f, TARGET);

        assertEquals(0f, result.width(), 0.01f);
        assertEquals(0f, result.height(), 0.01f);
        assertEquals(TARGET.left, result.left, 0.01f);
    }

    @Test
    public void topAlignedAjustaPeloMenorEscala() {
        Box result = ImageFit.topAligned(1000f, 500f, 523f, 770f, 36f, 36f);

        assertEquals(523f, result.width(), 0.01f);
        assertEquals(261.5f, result.height(), 0.01f);
        assertEquals(36f, result.top, 0.01f);
    }

    @Test
    public void panoramicaCabeNaLargura() {
        Box result = ImageFit.topAligned(2000f, 300f, 523f, 770f, 36f, 36f);

        assertEquals(523f, result.width(), 0.01f);
        assertEquals(78.45f, result.height(), 0.01f);
    }
}

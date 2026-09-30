package br.com.lelis718.docgen.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class TextTypesetterTest {

    private static final TextTypesetter.LineMeasurer TEN_PER_LINE =
            line -> line.length() * 10f;

    @Test
    public void quebraLinhasPorEspaco() {
        List<String> lines = TextTypesetter.wrap("aaaa bbbb cccc", 100f, TEN_PER_LINE);

        assertEquals(2, lines.size());
        assertEquals("aaaa bbbb", lines.get(0));
        assertEquals("cccc", lines.get(1));
    }

    @Test
    public void quebraPalavraLongaPorCaracter() {
        List<String> lines = TextTypesetter.wrap("aaaaaaaaaaaaaaaaaaaa", 100f, TEN_PER_LINE);

        assertEquals(2, lines.size());
        assertEquals(10, lines.get(0).length());
        assertEquals(10, lines.get(1).length());
    }

    @Test
    public void preservaLinhasEmBranco() {
        List<String> lines = TextTypesetter.wrap("a\n\nb", 1000f, TEN_PER_LINE);

        assertEquals(3, lines.size());
        assertEquals("a", lines.get(0));
        assertEquals("", lines.get(1));
        assertEquals("b", lines.get(2));
    }

    @Test
    public void normalizaQuebrasDeLinhaEDivisao() {
        List<String> lines = TextTypesetter.wrap("um\r\ndois\ttres", 1000f, TEN_PER_LINE);

        assertEquals(2, lines.size());
        assertEquals("um", lines.get(0));
        assertEquals("dois tres", lines.get(1));
    }

    @Test
    public void preservaIndentacaoDaPrimeiraLinha() {
        List<String> lines = TextTypesetter.wrap("    codigo", 1000f, TEN_PER_LINE);

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("    "));
        assertEquals("    codigo", lines.get(0));
    }

    @Test
    public void paragrafoVazioGeraLinhaVazia() {
        List<String> lines = TextTypesetter.wrap("a\n\n", 1000f, TEN_PER_LINE);

        assertEquals(3, lines.size());
        assertEquals("", lines.get(2));
    }

    @Test
    public void entradaNulaOuLarguraInvalidaGeraVazio() {
        assertTrue(TextTypesetter.wrap(null, 100f, TEN_PER_LINE).isEmpty());
        assertTrue(TextTypesetter.wrap("abc", 0f, TEN_PER_LINE).isEmpty());
    }

    @Test
    public void palavrasCabeNaLarguraNaoSaoQuebradas() {
        List<String> lines = TextTypesetter.wrap("uma frase curta", 1000f, TEN_PER_LINE);

        assertEquals(1, lines.size());
        assertEquals("uma frase curta", lines.get(0));
    }
}

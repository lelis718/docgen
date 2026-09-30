package br.com.lelis718.docgen.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDateTime;

public class FileNameRulesTest {

    @Test
    public void sanitizaCaracteresIlegais() {
        assertEquals("a b c d e f g h",
                FileNameRules.sanitize("a/b\\c:d*e?\"f<>g|h"));
        assertEquals("relatorio final.pdf", FileNameRules.sanitize("  relatorio   final.pdf "));
    }

    @Test
    public void entradaVaziaOuNulaRetornaVazio() {
        assertEquals("", FileNameRules.sanitize(null));
        assertEquals("", FileNameRules.sanitize(""));
        assertEquals("", FileNameRules.sanitize("   "));
        assertEquals("", FileNameRules.sanitize("..."));
        assertEquals("", FileNameRules.sanitize("///"));
    }

    @Test
    public void garanteExtensaoPdf() {
        assertEquals("relatorio.pdf", FileNameRules.ensurePdfExtension("relatorio"));
        assertEquals("relatorio.pdf", FileNameRules.ensurePdfExtension("relatorio.pdf"));
        assertEquals("relatorio.PDF", FileNameRules.ensurePdfExtension("relatorio.PDF"));
        assertEquals("", FileNameRules.ensurePdfExtension(""));
    }

    @Test
    public void truncaturaNomeMuitoLongo() {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longName.append('x');
        }

        String result = FileNameRules.ensurePdfExtension(FileNameRules.sanitize(longName.toString()));

        assertEquals(80 + ".pdf".length(), result.length());
        assertEquals("x.pdf", result.substring(result.length() - 5));
    }

    @Test
    public void nomePadraoUsaDataEHora() {
        assertEquals("DocGen_20260930_101530.pdf",
                FileNameRules.defaultName(LocalDateTime.of(2026, 9, 30, 10, 15, 30)));
        assertEquals("DocGen_20260101_000000.pdf",
                FileNameRules.defaultName(LocalDateTime.of(2026, 1, 1, 0, 0, 0)));
    }

    @Test
    public void removeExtensaoParaCalcularBase() {
        assertEquals("relatorio", FileNameRules.withoutExtension("relatorio.pdf"));
        assertEquals("relatorio", FileNameRules.withoutExtension("relatorio"));
        assertEquals("", FileNameRules.withoutExtension(".pdf"));
    }

    @Test
    public void nomeComPontoNoMeioEhPreservado() {
        assertEquals("v1.2 relatorio.pdf",
                FileNameRules.ensurePdfExtension(FileNameRules.sanitize("v1.2 relatorio")));
    }
}

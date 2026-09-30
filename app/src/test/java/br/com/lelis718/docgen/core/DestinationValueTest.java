package br.com.lelis718.docgen.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DestinationValueTest {

    @Test
    public void nullValueIsDownloads() {
        assertTrue(DestinationValue.isDownloads(null));
        assertFalse(DestinationValue.isDocuments(null));
        assertFalse(DestinationValue.isTree(null));
    }

    @Test
    public void downloadsValue() {
        assertTrue(DestinationValue.isDownloads(DestinationValue.DOWNLOADS));
        assertFalse(DestinationValue.isDocuments(DestinationValue.DOWNLOADS));
        assertFalse(DestinationValue.isTree(DestinationValue.DOWNLOADS));
    }

    @Test
    public void documentsValue() {
        assertTrue(DestinationValue.isDocuments(DestinationValue.DOCUMENTS));
        assertFalse(DestinationValue.isDownloads(DestinationValue.DOCUMENTS));
        assertFalse(DestinationValue.isTree(DestinationValue.DOCUMENTS));
    }

    @Test
    public void treeValueKeepsUri() {
        String uri = "content://com.android.externalstorage.documents/tree/primary%3ADocs";
        String value = DestinationValue.tree(uri);

        assertTrue(DestinationValue.isTree(value));
        assertFalse(DestinationValue.isDownloads(value));
        assertFalse(DestinationValue.isDocuments(value));
        assertEquals(uri, DestinationValue.uriOf(value));
    }

    @Test
    public void uriOfNonTreeIsEmpty() {
        assertEquals("", DestinationValue.uriOf(DestinationValue.DOWNLOADS));
        assertEquals("", DestinationValue.uriOf(DestinationValue.DOCUMENTS));
        assertEquals("", DestinationValue.uriOf(null));
    }
}

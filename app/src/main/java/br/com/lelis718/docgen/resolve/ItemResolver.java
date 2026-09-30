package br.com.lelis718.docgen.resolve;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;

import br.com.lelis718.docgen.core.Box;
import br.com.lelis718.docgen.core.ItemClassifier;
import br.com.lelis718.docgen.core.ItemKind;
import br.com.lelis718.docgen.core.Resolution;
import br.com.lelis718.docgen.core.SharedItem;
import br.com.lelis718.docgen.core.SourceDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

public final class ItemResolver {

    private static final long MAX_TEXT_BYTES = 2L * 1024 * 1024;

    private final ContentResolver resolver;

    public ItemResolver(ContentResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("resolver == null");
        }
        this.resolver = resolver;
    }

    public Resolution resolve(List<SharedItem> items) {
        List<SourceDescriptor> descriptors = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (items == null) {
            return new Resolution(descriptors, skipped);
        }
        for (SharedItem item : items) {
            try {
                resolveOne(item, descriptors, skipped);
            } catch (Exception e) {
                skipped.add(label(item));
            }
        }
        return new Resolution(descriptors, skipped);
    }

    private void resolveOne(SharedItem item, List<SourceDescriptor> descriptors,
                            List<String> skipped) throws IOException {
        if (!item.hasUri()) {
            String text = item.inlineText();
            if (text == null || text.trim().isEmpty()) {
                skipped.add("texto");
            } else {
                descriptors.add(SourceDescriptor.text(null, "texto compartilhado", text));
            }
            return;
        }

        String uri = item.uri();
        ItemKind kind = ItemClassifier.classify(resolveMime(uri, item.mimeType()), uri);
        if (kind == ItemKind.IMAGE) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream in = openStream(uri)) {
                BitmapFactory.decodeStream(in, null, options);
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                skipped.add(label(item));
                return;
            }
            descriptors.add(SourceDescriptor.image(uri, label(item), options.outWidth, options.outHeight));
            return;
        }
        if (kind == ItemKind.PDF) {
            List<Box> pageSizes = readPdfPageSizes(uri);
            if (pageSizes.isEmpty()) {
                skipped.add(label(item));
                return;
            }
            descriptors.add(SourceDescriptor.pdf(uri, label(item), pageSizes));
            return;
        }
        if (kind == ItemKind.TEXT) {
            String content = readText(uri);
            if (content.trim().isEmpty()) {
                skipped.add(label(item));
                return;
            }
            descriptors.add(SourceDescriptor.text(uri, label(item), content));
            return;
        }
        skipped.add(label(item));
    }

    private String resolveMime(String uri, String fallback) {
        String mime = null;
        try {
            mime = resolver.getType(Uri.parse(uri));
        } catch (Exception ignored) {
        }
        if (mime == null || mime.trim().isEmpty() || mime.equals("application/octet-stream")) {
            mime = fallback;
        }
        return mime;
    }

    private List<Box> readPdfPageSizes(String uri) throws IOException {
        List<Box> sizes = new ArrayList<>();
        try (ParcelFileDescriptor descriptor = openDescriptor(uri)) {
            if (descriptor == null) {
                return sizes;
            }
            try (PdfRenderer renderer = new PdfRenderer(descriptor)) {
                int count = renderer.getPageCount();
                for (int i = 0; i < count; i++) {
                    try (PdfRenderer.Page page = renderer.openPage(i)) {
                        sizes.add(Box.of(0f, 0f, page.getWidth(), page.getHeight()));
                    }
                }
            }
        } catch (SecurityException e) {
            return Collections.emptyList();
        }
        return sizes;
    }

    private String readText(String uri) throws IOException {
        try (InputStream in = openStream(uri)) {
            if (in == null) {
                return "";
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            long total = 0;
            int read;
            while (total < MAX_TEXT_BYTES && (read = in.read(chunk)) != -1) {
                int allowed = (int) Math.min(read, MAX_TEXT_BYTES - total);
                buffer.write(chunk, 0, allowed);
                total += allowed;
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private InputStream openStream(String uri) throws IOException {
        return resolver.openInputStream(Uri.parse(uri));
    }

    private ParcelFileDescriptor openDescriptor(String uri) throws IOException {
        return resolver.openFileDescriptor(Uri.parse(uri), "r");
    }

    private String label(SharedItem item) {
        if (!item.hasUri()) {
            return "texto compartilhado";
        }
        String uri = item.uri();
        try (Cursor cursor = resolver.query(Uri.parse(uri),
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String name = cursor.getString(column);
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        String lastSegment = Uri.parse(uri).getLastPathSegment();
        return lastSegment == null ? uri : lastSegment;
    }
}

package br.com.lelis718.docgen.pdf;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextPaint;

import br.com.lelis718.docgen.core.Box;
import br.com.lelis718.docgen.core.Element;
import br.com.lelis718.docgen.core.PdfPlan;
import br.com.lelis718.docgen.core.PlannedPage;
import br.com.lelis718.docgen.core.SourceDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public final class PdfAssembler {

    public static final float RENDER_SCALE = 2f;
    private static final int MAX_BITMAP_PIXELS = 4_000_000;

    private final ContentResolver resolver;
    private final Paint bitmapPaint;
    private final TextPaint textPaint;

    public PdfAssembler(ContentResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("resolver == null");
        }
        this.resolver = resolver;
        this.bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        this.textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        this.textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
    }

    public void write(PdfPlan plan, List<SourceDescriptor> sources, OutputStream output)
            throws IOException {
        PdfDocument document = new PdfDocument();
        RendererCache renderers = new RendererCache(resolver, sources);
        try {
            for (PlannedPage planned : plan.pages()) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                        Math.max(1, Math.round(planned.width())),
                        Math.max(1, Math.round(planned.height())),
                        planned.index()).create();
                PdfDocument.Page page = document.startPage(info);
                try {
                    Canvas canvas = page.getCanvas();
                    canvas.drawColor(Color.WHITE);
                    for (Element element : planned.elements()) {
                        draw(canvas, element, sources, renderers);
                    }
                } finally {
                    document.finishPage(page);
                }
            }
            document.writeTo(output);
            output.flush();
        } finally {
            renderers.close();
            document.close();
        }
    }

    private void draw(Canvas canvas, Element element, List<SourceDescriptor> sources,
                      RendererCache renderers) {
        switch (element.type()) {
            case IMAGE: {
                Bitmap bitmap = decodeImage(sources, element.itemIndex(), element.dest());
                blit(canvas, bitmap, element.dest());
                break;
            }
            case PDF_PAGE: {
                Box dest = element.dest();
                Bitmap bitmap = renderers.render(element.itemIndex(), element.sourcePageIndex(),
                        dest.width(), dest.height());
                blit(canvas, bitmap, dest);
                break;
            }
            case TEXT:
                drawText(canvas, element);
                break;
            default:
                break;
        }
    }

    private void blit(Canvas canvas, Bitmap bitmap, Box dest) {
        if (bitmap == null) {
            return;
        }
        try {
            canvas.drawBitmap(bitmap, null, toRect(dest), bitmapPaint);
        } finally {
            bitmap.recycle();
        }
    }

    private void drawText(Canvas canvas, Element element) {
        textPaint.setTextSize(element.fontSize());
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float glyphHeight = metrics.descent - metrics.ascent;
        Box dest = element.dest();
        List<String> lines = element.lines();
        for (int i = 0; i < lines.size(); i++) {
            float lineTop = dest.top + i * element.lineHeight();
            float baseline = lineTop + (element.lineHeight() - glyphHeight) / 2f - metrics.ascent;
            canvas.drawText(lines.get(i), dest.left, baseline, textPaint);
        }
    }

    private Bitmap decodeImage(List<SourceDescriptor> sources, int itemIndex, Box dest) {
        Uri uri = uriOf(sources, itemIndex);
        if (uri == null) {
            return null;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        } catch (Exception e) {
            return null;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight,
                pixelSize(dest.width()), pixelSize(dest.height()));
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream in = resolver.openInputStream(uri)) {
            return BitmapFactory.decodeStream(in, null, options);
        } catch (Exception e) {
            return null;
        }
    }

    private static Uri uriOf(List<SourceDescriptor> sources, int itemIndex) {
        String uri = sources.get(itemIndex).uri();
        return uri == null ? null : Uri.parse(uri);
    }

    private static int sampleSize(int width, int height, int targetWidth, int targetHeight) {
        int sample = 1;
        while (width / (sample * 2) >= targetWidth && height / (sample * 2) >= targetHeight) {
            sample *= 2;
        }
        return sample;
    }

    private static int pixelSize(float points) {
        return Math.max(1, Math.round(points * RENDER_SCALE));
    }

    private static RectF toRect(Box box) {
        return new RectF(box.left, box.top, box.right, box.bottom);
    }

    private static final class RendererCache {

        private final ContentResolver cacheResolver;
        private final List<SourceDescriptor> cacheSources;
        private int openIndex = -1;
        private ParcelFileDescriptor descriptor;
        private PdfRenderer renderer;

        RendererCache(ContentResolver cacheResolver, List<SourceDescriptor> cacheSources) {
            this.cacheResolver = cacheResolver;
            this.cacheSources = cacheSources;
        }

        Bitmap render(int itemIndex, int pageIndex, float widthPoints, float heightPoints) {
            try {
                ensureOpen(itemIndex);
            } catch (Exception e) {
                return null;
            }
            if (renderer == null) {
                return null;
            }
            int width = pixelSize(widthPoints);
            int height = pixelSize(heightPoints);
            long pixels = (long) width * height;
            if (pixels > MAX_BITMAP_PIXELS) {
                double factor = Math.sqrt((double) MAX_BITMAP_PIXELS / pixels);
                width = Math.max(1, (int) (width * factor));
                height = Math.max(1, (int) (height * factor));
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            try (PdfRenderer.Page page = renderer.openPage(pageIndex)) {
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                return bitmap;
            } catch (RuntimeException e) {
                bitmap.recycle();
                return null;
            }
        }

        private void ensureOpen(int itemIndex) throws IOException {
            if (itemIndex == openIndex && renderer != null) {
                return;
            }
            closeRenderer();
            String uri = cacheSources.get(itemIndex).uri();
            if (uri == null) {
                return;
            }
            descriptor = cacheResolver.openFileDescriptor(Uri.parse(uri), "r");
            if (descriptor == null) {
                return;
            }
            renderer = new PdfRenderer(descriptor);
            openIndex = itemIndex;
        }

        private void closeRenderer() {
            if (renderer != null) {
                try {
                    renderer.close();
                } catch (Exception ignored) {
                }
                renderer = null;
            }
            if (descriptor != null) {
                try {
                    descriptor.close();
                } catch (Exception ignored) {
                }
                descriptor = null;
            }
            openIndex = -1;
        }

        void close() {
            closeRenderer();
        }
    }
}

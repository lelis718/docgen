package br.com.lelis718.docgen;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import br.com.lelis718.docgen.share.ShareEntryActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ShareEntryActivityTest {

    @Test
    public void geraPdfUnicoComFotosDocumentosENaOrdemSelecionada() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        String fileName = "docgen-e2e-" + System.currentTimeMillis() + ".pdf";

        File inputs = new File(context.getCacheDir(), "outputs");
        assertTrue(inputs.isDirectory() || inputs.mkdirs());

        Uri photo1 = shareable(context, writePhoto(new File(inputs, "e2e-foto1.png"), Color.RED));
        Uri photo2 = shareable(context, writePhoto(new File(inputs, "e2e-foto2.png"), Color.GREEN));
        Uri document = shareable(context, writePdf(new File(inputs, "e2e-doc.pdf")));
        Uri notes = shareable(context,
                writeText(new File(inputs, "e2e-notas.txt"), "notas do teste\nsegunda linha"));

        Intent intent = new Intent(context, ShareEntryActivity.class);
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("*/*");
        ArrayList<Uri> streams = new ArrayList<>();
        streams.add(photo1);
        streams.add(photo2);
        streams.add(document);
        streams.add(notes);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, streams);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try (ActivityScenario<ShareEntryActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.input_file_name)).perform(replaceText(fileName));
            onView(withText(R.string.save_confirm)).perform(click());

            Uri output = waitForOutput(context, fileName, 60);
            assertNotNull("arquivo nao foi salvo no MediaStore", output);
            assertEquals("application/pdf", context.getContentResolver().getType(output));

            try (ParcelFileDescriptor descriptor =
                         context.getContentResolver().openFileDescriptor(output, "r");
                 PdfRenderer renderer = new PdfRenderer(descriptor)) {
                assertEquals(5, renderer.getPageCount());
                assertPageSize(renderer, 0, 595, 842);
                assertPageSize(renderer, 4, 595, 842);

                assertColorNear(Color.RED, centerPixel(renderer, 0));
                assertColorNear(Color.GREEN, centerPixel(renderer, 1));
                assertTrue(isMostlyWhite(renderer, 2));
                assertTrue(isMostlyWhite(renderer, 4));
            } finally {
                deleteOutput(context, output);
            }
        }
    }

    private static void assertPageSize(PdfRenderer renderer, int pageIndex, int width, int height)
            throws Exception {
        try (PdfRenderer.Page page = renderer.openPage(pageIndex)) {
            assertEquals(width, page.getWidth(), 3);
            assertEquals(height, page.getHeight(), 3);
        }
    }

    private static void assertColorNear(int expected, int actual) {
        String message = "esperado #" + Integer.toHexString(expected)
                + " mas veio #" + Integer.toHexString(actual);
        assertTrue(message, Math.abs(Color.red(expected) - Color.red(actual)) <= 16);
        assertTrue(message, Math.abs(Color.green(expected) - Color.green(actual)) <= 16);
        assertTrue(message, Math.abs(Color.blue(expected) - Color.blue(actual)) <= 16);
    }

    private static int centerPixel(PdfRenderer renderer, int pageIndex) throws Exception {
        try (PdfRenderer.Page page = renderer.openPage(pageIndex)) {
            int width = Math.min(page.getWidth(), 600);
            int height = Math.min(page.getHeight(), 850);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            try {
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                return bitmap.getPixel(width / 2, height / 3);
            } finally {
                bitmap.recycle();
            }
        }
    }

    private static boolean isMostlyWhite(PdfRenderer renderer, int pageIndex) throws Exception {
        int color = centerPixel(renderer, pageIndex);
        return Color.red(color) > 240 && Color.green(color) > 240 && Color.blue(color) > 240;
    }

    private static Uri waitForOutput(Context context, String fileName, int timeoutSeconds)
            throws InterruptedException {
        ContentResolver resolver = context.getContentResolver();
        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            try (Cursor cursor = resolver.query(collection,
                    new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.IS_PENDING},
                    MediaStore.MediaColumns.DISPLAY_NAME + "=?",
                    new String[]{fileName}, null)) {
                if (cursor != null && cursor.moveToFirst() && cursor.getInt(1) == 0) {
                    return ContentUris.withAppendedId(collection, cursor.getLong(0));
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(400);
        }
        return null;
    }

    private static void deleteOutput(Context context, Uri output) {
        try {
            context.getContentResolver().delete(output, null, null);
        } catch (Exception ignored) {
        }
    }

    private static Uri shareable(Context context, File file) {
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
    }

    private static File writePhoto(File file, int color) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally {
            bitmap.recycle();
        }
        return file;
    }

    private static File writePdf(File file) throws Exception {
        PdfDocument document = new PdfDocument();
        try {
            for (int i = 1; i <= 2; i++) {
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, i).create();
                PdfDocument.Page page = document.startPage(info);
                Paint paint = new Paint();
                paint.setColor(Color.BLACK);
                paint.setTextSize(24f);
                page.getCanvas().drawColor(Color.WHITE);
                page.getCanvas().drawText("Pagina " + i, 72f, 120f, paint);
                document.finishPage(page);
            }
            try (OutputStream out = new FileOutputStream(file)) {
                document.writeTo(out);
            }
        } finally {
            document.close();
        }
        return file;
    }

    private static File writeText(File file, String content) throws Exception {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }
}

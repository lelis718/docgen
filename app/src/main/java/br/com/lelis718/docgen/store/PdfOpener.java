package br.com.lelis718.docgen.store;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import br.com.lelis718.docgen.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public final class PdfOpener {

    private static final String TAG = "DocGen";
    private static final String OUTPUT_DIR = "outputs";

    private PdfOpener() {
    }

    public static boolean open(Context context, Uri target, String displayName) {
        for (int attempt = 0; attempt < 2; attempt++) {
            Uri uri = attempt == 0 ? target : copyToCache(context, target, displayName);
            if (uri == null) {
                continue;
            }
            Intent view = buildViewIntent(context, uri, displayName);
            if (!hasViewer(context, view)) {
                continue;
            }
            try {
                context.startActivity(Intent.createChooser(
                        view, context.getString(R.string.open_chooser_title)));
                return true;
            } catch (Exception e) {
                Log.w(TAG, "Falha ao abrir " + uri, e);
            }
        }
        return false;
    }

    private static Intent buildViewIntent(Context context, Uri uri, String displayName) {
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, "application/pdf");
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        view.setClipData(ClipData.newUri(context.getContentResolver(), displayName, uri));
        return view;
    }

    private static boolean hasViewer(Context context, Intent view) {
        PackageManager manager = context.getPackageManager();
        List<?> resolved = manager.queryIntentActivities(view, PackageManager.MATCH_DEFAULT_ONLY);
        return resolved != null && !resolved.isEmpty();
    }

    private static Uri copyToCache(Context context, Uri source, String displayName) {
        if (source == null) {
            return null;
        }
        File directory = new File(context.getCacheDir(), OUTPUT_DIR);
        if (!directory.exists() && !directory.mkdirs()) {
            return null;
        }
        File file = new File(directory, safeFileName(displayName));
        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(file)) {
            if (in == null) {
                return null;
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Falha ao copiar para cache", e);
            return null;
        }
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
    }

    private static String safeFileName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "documento.pdf";
        }
        return displayName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}

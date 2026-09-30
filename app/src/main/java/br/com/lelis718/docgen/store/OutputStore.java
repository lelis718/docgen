package br.com.lelis718.docgen.store;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;

public final class OutputStore {

    public enum Destination {
        DOWNLOADS,
        DOCUMENTS
    }

    private OutputStore() {
    }

    public static Uri create(ContentResolver resolver, Destination destination, String fileName)
            throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri collection;
        if (destination == Destination.DOCUMENTS) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);
            collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }

        Uri uri = resolver.insert(collection, values);
        if (uri == null) {
            throw new IOException("MediaStore insert returned null");
        }
        return uri;
    }

    public static Uri createInTree(ContentResolver resolver, String treeUri, String fileName)
            throws IOException {
        Uri tree = Uri.parse(treeUri);
        Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree,
                DocumentsContract.getTreeDocumentId(tree));
        Uri created = DocumentsContract.createDocument(resolver, parent, "application/pdf", fileName);
        if (created == null) {
            throw new IOException("DocumentsContract createDocument returned null");
        }
        return created;
    }

    public static OutputStream open(ContentResolver resolver, Uri target) throws IOException {
        OutputStream stream = resolver.openOutputStream(target, "w");
        if (stream == null) {
            throw new IOException("openOutputStream returned null for " + target);
        }
        return stream;
    }

    public static void commit(ContentResolver resolver, Uri target) {
        if (target == null || !ContentResolver.SCHEME_CONTENT.equals(target.getScheme())) {
            return;
        }
        if (isMediaStore(target)) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            try {
                resolver.update(target, values, null, null);
            } catch (Exception ignored) {
            }
        }
    }

    public static void delete(ContentResolver resolver, Uri target) {
        if (target == null || !ContentResolver.SCHEME_CONTENT.equals(target.getScheme())) {
            return;
        }
        if (isMediaStore(target)) {
            try {
                resolver.delete(target, null, null);
            } catch (Exception ignored) {
            }
        } else {
            try {
                DocumentsContract.deleteDocument(resolver, target);
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isMediaStore(Uri target) {
        String authority = target.getAuthority();
        return MediaStore.AUTHORITY.equals(authority);
    }
}

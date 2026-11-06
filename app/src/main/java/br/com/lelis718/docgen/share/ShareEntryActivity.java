package br.com.lelis718.docgen.share;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;


import br.com.lelis718.docgen.R;
import br.com.lelis718.docgen.core.CompressionLevel;
import br.com.lelis718.docgen.core.DestinationValue;
import br.com.lelis718.docgen.core.FileNameRules;
import br.com.lelis718.docgen.core.PageGeometry;
import br.com.lelis718.docgen.core.PdfLayoutPlanner;
import br.com.lelis718.docgen.core.PdfPlan;
import br.com.lelis718.docgen.core.Resolution;
import br.com.lelis718.docgen.core.ShareParser;
import br.com.lelis718.docgen.core.SharedItem;
import br.com.lelis718.docgen.pdf.PdfAssembler;
import br.com.lelis718.docgen.resolve.ItemResolver;
import br.com.lelis718.docgen.store.DestinationPrefs;
import br.com.lelis718.docgen.store.OutputStore;
import br.com.lelis718.docgen.store.PdfOpener;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ShareEntryActivity extends Activity {

    private static final String TAG = "DocGen";
    private static final int REQUEST_CHOOSE_LOCATION = 1;
    private static final String STATE_OUTPUT_URI = "state.outputUri";
    private static final String STATE_OUTPUT_NAME = "state.outputName";
    private static final String STATE_COMPRESSION = "state.compression";

    private final List<SharedItem> sharedItems = new ArrayList<>();
    private ExecutorService executor;
    private Handler mainHandler;
    private AlertDialog progressDialog;
    private Uri outputUri;
    private String outputName;
    private CompressionLevel compression = CompressionLevel.defaultLevel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        collectSharedItems(getIntent());

        if (savedInstanceState != null) {
            outputUri = readParcelable(savedInstanceState);
            outputName = savedInstanceState.getString(STATE_OUTPUT_NAME);
            compression = CompressionLevel.fromIndex(savedInstanceState.getInt(STATE_COMPRESSION,
                    CompressionLevel.defaultLevel().ordinal()));
        }

        if (sharedItems.isEmpty()) {
            toast(R.string.error_no_files);
            finish();
            return;
        }
        if (outputUri != null) {
            showProgress();
            generate();
        } else {
            showSaveDialog();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_COMPRESSION, compression.ordinal());
        if (outputUri != null) {
            outState.putParcelable(STATE_OUTPUT_URI, outputUri);
            outState.putString(STATE_OUTPUT_NAME, outputName);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    @SuppressWarnings("deprecation")
    private Uri readParcelable(Bundle state) {
        return state.getParcelable(STATE_OUTPUT_URI);
    }

    @SuppressWarnings("deprecation")
    private void collectSharedItems(Intent intent) {
        sharedItems.clear();
        if (intent == null) {
            return;
        }
        List<String> uris = new ArrayList<>();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri single = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (single != null) {
                uris.add(single.toString());
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> many = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (many != null) {
                for (Uri uri : many) {
                    if (uri != null) {
                        uris.add(uri.toString());
                    }
                }
            }
            if (uris.isEmpty()) {
                Uri single = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (single != null) {
                    uris.add(single.toString());
                }
            }
        }
        if (uris.isEmpty() && intent.getClipData() != null) {
            ClipData clipData = intent.getClipData();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null) {
                    uris.add(uri.toString());
                }
            }
        }
        if (uris.isEmpty() && intent.getData() != null) {
            uris.add(intent.getData().toString());
        }

        sharedItems.addAll(ShareParser.parse(uris, intent.getType()));
        if (sharedItems.isEmpty()) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null && !text.trim().isEmpty()) {
                sharedItems.add(ShareParser.text(text, intent.getType()));
            }
        }
    }

    private void showSaveDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_save, null);
        EditText nameInput = content.findViewById(R.id.input_file_name);
        RadioGroup locations = content.findViewById(R.id.radio_location);
        Spinner compressionInput = content.findViewById(R.id.spinner_compression);
        nameInput.setText(FileNameRules.defaultName());
        compressionInput.setSelection(compression.ordinal());
        applyDefaultDestination(content);

        AlertDialog dialog = dialogBuilder()
                .setTitle(R.string.save_title)
                .setMessage(R.string.save_message)
                .setView(content)
                .setPositiveButton(R.string.save_confirm, null)
                .setNegativeButton(R.string.save_cancel, (d, w) -> finish())
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v ->
                        onSaveConfirmed(dialog, nameInput, locations, compressionInput)));
        dialog.show();
    }

    private void applyDefaultDestination(View content) {
        RadioGroup locations = content.findViewById(R.id.radio_location);
        String destination = DestinationPrefs.read(this);
        if (DestinationValue.isTree(destination)) {
            RadioButton tree = content.findViewById(R.id.radio_default_tree);
            String name = DestinationPrefs.treeName(this);
            tree.setText(getString(R.string.save_location_tree,
                    name.trim().isEmpty() ? getString(R.string.setup_tree) : name));
            tree.setVisibility(View.VISIBLE);
            tree.setChecked(true);
        } else if (DestinationValue.isDocuments(destination)) {
            locations.check(R.id.radio_documents);
        } else {
            locations.check(R.id.radio_downloads);
        }
    }

    private void onSaveConfirmed(AlertDialog dialog, EditText nameInput, RadioGroup locations,
                                 Spinner compressionInput) {
        String safeName = FileNameRules.sanitize(nameInput.getText().toString());
        if (safeName.isEmpty()) {
            nameInput.setError(getString(R.string.error_invalid_name));
            return;
        }
        dialog.dismiss();
        compression = CompressionLevel.fromIndex(compressionInput.getSelectedItemPosition());
        String fileName = FileNameRules.ensurePdfExtension(safeName);
        int selected = locations.getCheckedRadioButtonId();
        if (selected == R.id.radio_choose) {
            launchSystemPicker(fileName);
            return;
        }
        if (selected == R.id.radio_default_tree) {
            saveInTree(fileName);
            return;
        }
        OutputStore.Destination destination = selected == R.id.radio_documents
                ? OutputStore.Destination.DOCUMENTS
                : OutputStore.Destination.DOWNLOADS;
        try {
            Uri uri = OutputStore.create(getContentResolver(), destination, fileName);
            startGeneration(uri, fileName);
        } catch (Exception e) {
            Log.e(TAG, "Falha ao criar arquivo no MediaStore", e);
            toast(R.string.error_save);
            finish();
        }
    }

    private void saveInTree(String fileName) {
        String destination = DestinationValue.uriOf(DestinationPrefs.read(this));
        if (destination.isEmpty()) {
            toast(R.string.error_save);
            finish();
            return;
        }
        try {
            Uri uri = OutputStore.createInTree(getContentResolver(), destination, fileName);
            startGeneration(uri, fileName);
        } catch (Exception e) {
            Log.e(TAG, "Falha ao criar arquivo na pasta escolhida", e);
            toast(R.string.error_save);
            finish();
        }
    }

    private void launchSystemPicker(String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        try {
            startActivityForResult(intent, REQUEST_CHOOSE_LOCATION);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Nenhum seletor de arquivos disponivel", e);
            toast(R.string.error_save);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CHOOSE_LOCATION) {
            return;
        }
        Uri uri = data == null ? null : data.getData();
        if (resultCode != RESULT_OK || uri == null) {
            finish();
            return;
        }
        String name = queryDisplayName(uri);
        startGeneration(uri, name);
    }

    private String queryDisplayName(Uri uri) {
        try (android.database.Cursor cursor = getContentResolver().query(uri,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String value = cursor.getString(column);
                    if (value != null && !value.trim().isEmpty()) {
                        return value;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        String lastSegment = uri.getLastPathSegment();
        return lastSegment == null ? "documento.pdf" : lastSegment;
    }

    private void startGeneration(Uri uri, String fileName) {
        outputUri = uri;
        outputName = fileName;
        showProgress();
        executor.execute(this::generate);
    }

    private void generate() {
        try {
            ItemResolver itemResolver = new ItemResolver(getContentResolver());
            Resolution resolution = itemResolver.resolve(sharedItems);
            if (!resolution.skippedNames().isEmpty()) {
                Log.w(TAG, "Itens ignorados: " + String.join(", ", resolution.skippedNames()));
            }
            if (resolution.isEmpty()) {
                fail(getString(R.string.error_unsupported));
                return;
            }

            TextPaint measurePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            measurePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            measurePaint.setTextSize(PdfLayoutPlanner.TEXT_FONT_SIZE);

            PdfLayoutPlanner planner =
                    new PdfLayoutPlanner(line -> measurePaint.measureText(line));
            PdfPlan plan = planner.plan(resolution.items(), PageGeometry.A4);
            if (plan.isEmpty()) {
                fail(getString(R.string.error_unsupported));
                return;
            }

            try (OutputStream output = OutputStore.open(getContentResolver(), outputUri)) {
                new PdfAssembler(getContentResolver(), compression)
                        .write(plan, resolution.items(), output);
            }
            OutputStore.commit(getContentResolver(), outputUri);
            succeed();
        } catch (Exception e) {
            Log.e(TAG, "Falha ao gerar PDF", e);
            OutputStore.delete(getContentResolver(), outputUri);
            fail(getString(R.string.error_generation, messageOf(e)));
        }
    }

    private void succeed() {
        mainHandler.post(() -> {
            dismissProgress();
            Uri uri = outputUri;
            String name = outputName;
            boolean opened = uri != null && PdfOpener.open(this, uri, name);
            if (opened) {
                toast(getString(R.string.created_toast, name));
            } else {
                toast(getString(R.string.error_open, name == null ? "" : name));
            }
            finish();
        });
    }

    private void fail(String message) {
        mainHandler.post(() -> {
            dismissProgress();
            try {
                dialogBuilder()
                        .setTitle(R.string.progress_title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                        .show();
            } catch (Exception e) {
                Log.w(TAG, "Nao foi possivel exibir o erro", e);
                finish();
            }
        });
    }

    private AlertDialog.Builder dialogBuilder() {
        return new AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
    }

    private void showProgress() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        dismissProgress();
        View content = LayoutInflater.from(this).inflate(R.layout.view_progress, null);
        progressDialog = dialogBuilder()
                .setTitle(R.string.progress_title)
                .setView(content)
                .setCancelable(false)
                .create();
        progressDialog.show();
    }

    private void dismissProgress() {
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (Exception ignored) {
            }
            progressDialog = null;
        }
    }

    private void toast(int message) {
        toast(getString(message));
    }

    private void toast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    private static String messageOf(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return error.getClass().getSimpleName();
        }
        return message;
    }
}

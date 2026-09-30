package br.com.lelis718.docgen.share;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import br.com.lelis718.docgen.R;
import br.com.lelis718.docgen.core.DestinationValue;
import br.com.lelis718.docgen.store.DestinationPrefs;

public final class SetupActivity extends Activity {

    private static final int REQUEST_PICK_TREE = 1;

    private AlertDialog dialog;
    private RadioGroup destinations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showSetupDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismiss();
    }

    private void showSetupDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        dismiss();

        View content = LayoutInflater.from(this).inflate(R.layout.dialog_setup, null);
        destinations = content.findViewById(R.id.radio_setup_destination);
        bindDestination();

        destinations.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_setup_tree
                    && !DestinationValue.isTree(DestinationPrefs.read(this))) {
                pickTree();
            }
        });

        dialog = new AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(R.string.setup_title)
                .setMessage(R.string.setup_instructions)
                .setView(content)
                .setCancelable(true)
                .setPositiveButton(R.string.setup_save, (d, w) -> save())
                .setNegativeButton(R.string.setup_close, (d, w) -> finish())
                .create();
        dialog.setOnCancelListener(d -> finish());
        dialog.show();
    }

    private void bindDestination() {
        String current = DestinationPrefs.read(this);
        if (DestinationValue.isTree(current)) {
            android.widget.RadioButton tree = destinations.findViewById(R.id.radio_setup_tree);
            String name = DestinationPrefs.treeName(this);
            tree.setText(getString(R.string.save_location_tree,
                    name.trim().isEmpty() ? getString(R.string.setup_tree) : name));
            tree.setChecked(true);
        } else if (DestinationValue.isDocuments(current)) {
            destinations.check(R.id.radio_setup_documents);
        } else {
            destinations.check(R.id.radio_setup_downloads);
        }
    }

    private void save() {
        String current = DestinationPrefs.read(this);
        String name = DestinationPrefs.treeName(this);
        String destination;
        int selected = destinations.getCheckedRadioButtonId();
        if (selected == R.id.radio_setup_documents) {
            destination = DestinationValue.DOCUMENTS;
        } else if (selected == R.id.radio_setup_tree && DestinationValue.isTree(current)) {
            destination = current;
        } else {
            destination = DestinationValue.DOWNLOADS;
        }
        DestinationPrefs.write(this, destination,
                DestinationValue.isTree(destination) ? name : "");
        toast(R.string.setup_saved);
        finish();
    }

    private void pickTree() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_PICK_TREE);
        } catch (Exception e) {
            showSetupDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_TREE) {
            return;
        }
        Uri tree = data == null ? null : data.getData();
        if (resultCode != RESULT_OK || tree == null) {
            showSetupDialog();
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(tree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception e) {
            showSetupDialog();
            return;
        }
        DestinationPrefs.write(this, DestinationValue.tree(tree.toString()), queryTreeName(tree));
        showSetupDialog();
    }

    private String queryTreeName(Uri tree) {
        try {
            Uri document = DocumentsContract.buildDocumentUriUsingTree(tree,
                    DocumentsContract.getTreeDocumentId(tree));
            try (Cursor cursor = getContentResolver().query(document,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String name = cursor.getString(0);
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private void dismiss() {
        if (dialog != null) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {
            }
            dialog = null;
        }
    }

    private void toast(int message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }
}

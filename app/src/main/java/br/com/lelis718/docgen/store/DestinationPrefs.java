package br.com.lelis718.docgen.store;

import android.content.Context;
import android.content.SharedPreferences;

import br.com.lelis718.docgen.core.DestinationValue;

public final class DestinationPrefs {

    private static final String PREFERENCES = "docgen_prefs";
    private static final String KEY_DESTINATION = "destination";
    private static final String KEY_TREE_NAME = "tree_name";

    private DestinationPrefs() {
    }

    public static String read(Context context) {
        String value = preferences(context).getString(KEY_DESTINATION, DestinationValue.DOWNLOADS);
        return value == null || value.trim().isEmpty() ? DestinationValue.DOWNLOADS : value;
    }

    public static String treeName(Context context) {
        String value = preferences(context).getString(KEY_TREE_NAME, "");
        return value == null ? "" : value;
    }

    public static void write(Context context, String destination, String treeName) {
        preferences(context).edit()
                .putString(KEY_DESTINATION, destination)
                .putString(KEY_TREE_NAME, treeName == null ? "" : treeName)
                .apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}

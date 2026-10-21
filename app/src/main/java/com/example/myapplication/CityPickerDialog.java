package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Offline city picker backed by the bundled Cities States Countries dataset.
 * Database queries run on a background thread; only a small result page is held in memory.
 */
public final class CityPickerDialog {
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int MAX_RESULTS = 60;

    private CityPickerDialog() {}

    public interface TimeZoneResolutionCallback {
        void onResolved(String timeZoneId);
    }


    /**
     * Best-effort offline time-zone resolution for a GPS position.
     *
     * This is intentionally coarse: it uses the nearest city record in the
     * bundled city database. A GPS fix can be far from that city, so the result
     * is a practical fallback only. The caller must still tell the user when an
     * accurate zone could not be determined and allow manual selection.
     */
    private static String findNearestTimeZoneSync(Context context, double latitude, double longitude) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0
                || !Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            return null;
        }
        try {
            File file = CityDatabaseHelper.copyDatabaseIfNeeded(context);
            db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            cursor = db.rawQuery(
                    "SELECT latitude, longitude, timezone FROM cities",
                    null);
            double targetLat = Math.toRadians(latitude);
            double targetLon = Math.toRadians(longitude);
            double bestDistance = Double.MAX_VALUE;
            String bestZone = null;
            while (cursor != null && cursor.moveToNext()) {
                double cityLat = cursor.getDouble(0);
                double cityLon = cursor.getDouble(1);
                String zone = cursor.getString(2);
                if (zone == null || zone.trim().isEmpty()) {
                    continue;
                }
                String candidate = zone.trim();
                try {
                    java.time.ZoneId.of(candidate);
                } catch (Exception ignored) {
                    continue;
                }
                double cityLatRad = Math.toRadians(cityLat);
                double cityLonRad = Math.toRadians(cityLon);
                double deltaLat = targetLat - cityLatRad;
                double deltaLon = targetLon - cityLonRad;
                double a = Math.sin(deltaLat / 2.0) * Math.sin(deltaLat / 2.0)
                        + Math.cos(cityLatRad) * Math.cos(targetLat)
                        * Math.sin(deltaLon / 2.0) * Math.sin(deltaLon / 2.0);
                if (a < 0.0) a = 0.0;
                if (a > 1.0) a = 1.0;
                double centralAngle = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
                if (centralAngle < bestDistance) {
                    bestDistance = centralAngle;
                    bestZone = candidate;
                }
            }
            return bestZone;
        } catch (Exception e) {
            return null;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    /**
     * Resolve a best-effort time zone without blocking the calling thread.
     *
     * The database lookup runs on the shared database executor. The result is
     * delivered on the main thread. Callers must guard against destroyed
     * activities and duplicate requests.
     */
    public static void resolveNearestTimeZone(Context context, double latitude, double longitude,
                                             TimeZoneResolutionCallback callback) {
        final Context appContext = (context == null) ? null : context.getApplicationContext();
        DB_EXECUTOR.execute(() -> {
            String resolved = null;
            if (appContext != null) {
                resolved = findNearestTimeZoneSync(appContext, latitude, longitude);
            }
            final String result = resolved;
            MAIN.post(() -> {
                if (callback != null) {
                    callback.onResolved(result);
                }
            });
        });
    }


    public interface SelectionListener {
        void onCitySelected(City city);
    }

    public static final class City {
        public final String name;
        public final String stateName;
        public final String countryName;
        public final String countryCode;
        public final double latitude;
        public final double longitude;
        public final String timezone;
        public final String type;

        City(String name, String stateName, String countryName, String countryCode,
             double latitude, double longitude, String timezone, String type) {
            this.name = name;
            this.stateName = stateName;
            this.countryName = countryName;
            this.countryCode = countryCode;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timezone = timezone;
            this.type = type;
        }

        @Override public String toString() {
            StringBuilder label = new StringBuilder(name);
            if (stateName != null && !stateName.isEmpty()
                    && !stateName.equalsIgnoreCase(name)) {
                label.append(" — ").append(stateName);
            }
            label.append(", ").append(countryName);
            return label.toString();
        }
    }

    public static void show(Activity activity, String currentCity, SelectionListener listener) {
        if (activity == null || activity.isFinishing()) return;

        float density = activity.getResources().getDisplayMetrics().density;
        int pad = (int) (18 * density);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad / 2, pad, 0);

        EditText search = new EditText(activity);
        search.setSingleLine(true);
        search.setHint(currentCity == null || currentCity.trim().isEmpty()
                ? "Search city, region or country"
                : "Search city, region or country (current: " + currentCity + ")");
        root.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView status = new TextView(activity);
        status.setText("Type at least 2 characters. Works offline.");
        status.setTextSize(13);
        status.setPadding(0, (int) (8 * density), 0, (int) (8 * density));
        root.addView(status);

        ListView list = new ListView(activity);
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (330 * density)));

        TextView attribution = new TextView(activity);
        attribution.setText("City data: Countries States Cities Database · ODbL v1.0");
        attribution.setTextSize(10);
        attribution.setGravity(Gravity.CENTER_VERTICAL);
        attribution.setPadding(0, (int) (8 * density), 0, (int) (8 * density));
        root.addView(attribution);

        ArrayList<City> results = new ArrayList<>();
        ArrayAdapter<City> adapter = new ArrayAdapter<City>(activity,
                android.R.layout.simple_list_item_1, results) {
            @Override public View getView(int position, View convertView,
                                          android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setPadding((int) (10 * density), (int) (12 * density),
                        (int) (10 * density), (int) (12 * density));
                view.setTextSize(15);
                return view;
            }
        };
        list.setAdapter(adapter);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Choose a city")
                .setView(root)
                .setNegativeButton("Cancel", null)
                .create();

        final Runnable[] pending = new Runnable[1];
        final int[] requestVersion = {0};

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                int version = ++requestVersion[0];
                if (pending[0] != null) MAIN.removeCallbacks(pending[0]);

                if (query.length() < 2) {
                    results.clear();
                    adapter.notifyDataSetChanged();
                    status.setText("Type at least 2 characters. Works offline.");
                    return;
                }

                status.setText("Searching offline…");
                pending[0] = () -> DB_EXECUTOR.execute(() -> {
                    List<City> found;
                    try {
                        found = CityDatabaseHelper.search(activity.getApplicationContext(), query);
                    } catch (Exception e) {
                        found = new ArrayList<>();
                    }
                    List<City> finalFound = found;
                    MAIN.post(() -> {
                        if (!dialog.isShowing() || version != requestVersion[0]) return;
                        results.clear();
                        results.addAll(finalFound);
                        adapter.notifyDataSetChanged();
                        if (finalFound.isEmpty()) {
                            status.setText("No matches. Try another spelling.");
                        } else {
                            status.setText(finalFound.size() + " results · Select your city");
                        }
                    });
                });
                MAIN.postDelayed(pending[0], 250);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        search.addTextChangedListener(watcher);

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < results.size()) {
                City selected = results.get(position);
                dialog.dismiss();
                if (listener != null) listener.onCitySelected(selected);
            }
        });

        dialog.setOnShowListener(d -> {
            // Re-trigger search after the dialog is visible, including for the prefilled city.
            if (search.getText().length() >= 2) {
                search.setSelection(search.getText().length());
                search.setText(search.getText().toString());
            } else {
                search.requestFocus();
            }
        });
        dialog.show();
    }

    private static final class CityDatabaseHelper {
        private static final String DB_NAME = "cities.db";

        static List<City> search(Context context, String rawQuery) throws IOException {
            File file = copyDatabaseIfNeeded(context);
            String query = normalize(rawQuery);
            if (query.length() < 2) return new ArrayList<>();

            SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            try {
                List<City> matches = runQuery(db, query, false);
                if (matches.size() < 8 && query.length() >= 3) {
                    List<City> contains = runQuery(db, query, true);
                    for (City city : contains) {
                        boolean duplicate = false;
                        for (City old : matches) {
                            if (old.name.equals(city.name)
                                    && old.countryName.equals(city.countryName)
                                    && equal(old.stateName, city.stateName)) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate && matches.size() < MAX_RESULTS) matches.add(city);
                    }
                }
                return matches;
            } finally {
                db.close();
            }
        }

        private static List<City> runQuery(SQLiteDatabase db, String query, boolean contains) {
            List<City> out = new ArrayList<>();
            String pattern = contains ? "%" + query + "%" : query + "%";
            String sql = "SELECT name,state_name,country_name,country_code,latitude,longitude,timezone,type "
                    + "FROM cities WHERE name_search LIKE ? OR state_search LIKE ? OR country_search LIKE ? "
                    + "ORDER BY CASE WHEN country_search = ? THEN 0 WHEN name_search = ? THEN 1 "
                    + "WHEN name_search LIKE ? THEN 2 ELSE 3 END, name COLLATE NOCASE LIMIT " + MAX_RESULTS;
            try (Cursor c = db.rawQuery(sql, new String[] {
                    pattern, pattern, pattern, query, query, query + "%"
            })) {
                while (c.moveToNext()) {
                    out.add(new City(c.getString(0), c.getString(1), c.getString(2),
                            c.getString(3), c.getDouble(4), c.getDouble(5),
                            c.getString(6), c.getString(7)));
                }
            }
            return out;
        }

        private static File copyDatabaseIfNeeded(Context context) throws IOException {
            File target = context.getDatabasePath(DB_NAME);
            if (target.exists() && target.length() > 1024 * 1024) return target;
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Could not create database directory");
            }
            File temp = new File(target.getAbsolutePath() + ".tmp");
            try (InputStream in = context.getAssets().open(DB_NAME);
                 FileOutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[32 * 1024];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                out.getFD().sync();
            }
            if (target.exists() && !target.delete()) throw new IOException("Could not replace city database");
            if (!temp.renameTo(target)) throw new IOException("Could not install city database");
            return target;
        }

        private static String normalize(String value) {
            String lower = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFKD);
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < decomposed.length(); i++) {
                char ch = decomposed.charAt(i);
                int type = Character.getType(ch);
                if (type != Character.NON_SPACING_MARK
                        && type != Character.COMBINING_SPACING_MARK
                        && type != Character.ENCLOSING_MARK) out.append(ch);
            }
            return out.toString().trim();
        }

        private static boolean equal(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }
    }
}

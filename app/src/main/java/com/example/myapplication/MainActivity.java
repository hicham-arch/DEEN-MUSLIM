package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.DateTimeException;

public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCES_NAME = "deen_muslim_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private PrayerPrefs prefs;
    private TextView prayerLocation, currentPrayerTime, currentPrayerName, remainingTime;
    private TextView fajrTime, dhuhrTime, asrTime, maghribTime, ishaTime;
    private com.google.android.material.card.MaterialCardView fajrCard, dhuhrCard, asrCard, maghribCard, ishaCard;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable remainingTimeUpdater;
    private PrayerTimings currentTimings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PrayerPrefs(this);

        bindViews();
        updatePrayerDate();
        setupFeatureGrid();

        //findViewById(R.id.appearanceButton).setOnClickListener(v -> showAppearanceDialog());
        findViewById(R.id.appearanceButton).setOnClickListener(v -> showSettingsDialog());

        // Start loading prayer times
        loadPrayerTimes();
    }
    
    // =====================================================
// SETTINGS DIALOG
// =====================================================

private void showSettingsDialog() {
    // Custom layout for professional look
    android.widget.LinearLayout root = new android.widget.LinearLayout(this);
    root.setOrientation(android.widget.LinearLayout.VERTICAL);
    int pad = (int) (20 * getResources().getDisplayMetrics().density);
    root.setPadding(pad, pad, pad, pad / 2);

    // Helper to create each row
    java.util.function.Function<String[], android.view.View> createRow = (data) -> {
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, pad / 2, 0, pad / 2);
        row.setClickable(true);
        row.setFocusable(true);

        // Icon circle
        android.widget.TextView icon = new android.widget.TextView(this);
        icon.setText(data[0]);
        icon.setTextSize(20);
        icon.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout.LayoutParams iconLp =
                new android.widget.LinearLayout.LayoutParams(
                        (int) (42 * getResources().getDisplayMetrics().density),
                        (int) (42 * getResources().getDisplayMetrics().density));
        iconLp.setMarginEnd(pad);
        icon.setLayoutParams(iconLp);
        row.addView(icon);

        // Title + subtitle
        android.widget.LinearLayout textCol = new android.widget.LinearLayout(this);
        textCol.setOrientation(android.widget.LinearLayout.VERTICAL);
        textCol.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(data[1]);
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        android.util.TypedValue titleColor = new android.util.TypedValue();
        getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurface,
            titleColor,
            true
        );
        title.setTextColor(titleColor.data);
        textCol.addView(title);

        android.widget.TextView subtitle = new android.widget.TextView(this);
        subtitle.setText(data[2]);
        subtitle.setTextSize(13);
        //subtitle.setTextColor(0xFFAAAAAA);
        android.util.TypedValue subtitleColor = new android.util.TypedValue();
        getTheme().resolveAttribute(
          com.google.android.material.R.attr.colorOnSurfaceVariant,
          subtitleColor,
          true
        );
        subtitle.setTextColor(subtitleColor.data);
        textCol.addView(subtitle);

        row.addView(textCol);

        // Arrow
        android.widget.TextView arrow = new android.widget.TextView(this);
        arrow.setText("›");
        arrow.setTextSize(22);
        arrow.setTextColor(0xFF888888);
        row.addView(arrow);

        return row;
    };

    // Current values for subtitles
    String locationSubtitle = prefs.isUseGps()
            ? "Using GPS"
            : prefs.getCity() + ", " + prefs.getCountry();

    String methodSubtitle = getMethodName(prefs.getMethod());

    // Rows
    android.view.View locationRow = createRow.apply(new String[]{"📍", "Location", locationSubtitle});
    android.view.View methodRow   = createRow.apply(new String[]{"🧮", "Calculation Method", methodSubtitle});
    android.view.View themeRow    = createRow.apply(new String[]{"🎨", "Appearance", "Light / Dark / System"});

    root.addView(locationRow);
    root.addView(methodRow);
    root.addView(themeRow);

    // Click listeners
    locationRow.setOnClickListener(v -> {
        showLocationDialog();
    });
    methodRow.setOnClickListener(v -> {
        showMethodDialog();
    });
    themeRow.setOnClickListener(v -> {
        showAppearanceDialog();
    });

    new MaterialAlertDialogBuilder(this)
            .setTitle("Settings")
            .setView(root)
            .setNegativeButton("Close", null)
            .show();
}

// Helper to show method name
private String getMethodName(int method) {
    switch (method) {
        case 1:  return "Karachi";
        case 2:  return "ISNA";
        case 3:  return "Muslim World League";
        case 4:  return "Umm Al-Qura";
        case 5:  return "Egyptian";
        case 7:  return "Tehran";
        case 8:  return "Gulf Region";
        case 9:  return "Kuwait";
        case 10: return "Qatar";
        case 11: return "Singapore";
        case 12: return "France";
        case 13: return "Turkey (Diyanet)";
        case 14: return "Russia";
        default: return "ISNA";
    }
}
// -----------------------------------------------------
// LOCATION
// -----------------------------------------------------

private void showLocationDialog() {
    String current = prefs.isUseGps()
            ? "Currently using GPS"
            : "Currently: " + prefs.getCity() + ", " + prefs.getCountry();

    String[] options = {
            "Use GPS (Automatic)",
            "Enter City manually"
    };

    new MaterialAlertDialogBuilder(this)
            .setTitle("Change Location")
            .setMessage(current)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    prefs.setUseGps(true);
                    Toast.makeText(this, "Switching to GPS…", Toast.LENGTH_SHORT).show();
                    requestLocationAndFetch();
                } else {
                    showCityInputDialog();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
}

private void showCityInputDialog() {
    CityPickerDialog.show(this, prefs.getCity(), selected -> {

        prefs.setCity(selected.name);
        prefs.setCountry(selected.countryName);
        prefs.setUseGps(false);

        // Save the selected city's coordinates.
        prefs.saveLastLocation(
                selected.latitude,
                selected.longitude
        );
        prefs.setTimeZone(selected.timezone);

        // Calculate using the selected city's actual time zone.
        calculatePrayerTimesOffline(
                selected.latitude,
                selected.longitude,
                selected.timezone
        );
    });
}

// -----------------------------------------------------
// CALCULATION METHOD
// -----------------------------------------------------

private void showMethodDialog() {
    // Aladhan method IDs
    final String[] methodNames = {
            "University of Islamic Sciences, Karachi (1)",
            "Islamic Society of North America - ISNA (2)",
            "Muslim World League (3)",
            "Umm Al-Qura University, Makkah (4)",
            "Egyptian General Authority of Survey (5)",
            "Institute of Geophysics, University of Tehran (7)",
            "Gulf Region (8)",
            "Kuwait (9)",
            "Qatar (10)",
            "Majlis Ugama Islam Singapura (11)",
            "Union Organization islamic de France (12)",
            "Diyanet İşleri Başkanlığı, Turkey (13)",
            "Spiritual Administration of Muslims of Russia (14)"
    };

    final int[] methodIds = {1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14};

    int currentMethod = prefs.getMethod();
    int checkedItem = 1; // default ISNA
    for (int i = 0; i < methodIds.length; i++) {
        if (methodIds[i] == currentMethod) {
            checkedItem = i;
            break;
        }
    }

    new MaterialAlertDialogBuilder(this)
            .setTitle("Calculation Method")
            .setSingleChoiceItems(methodNames, checkedItem, (dialog, which) -> {
                prefs.setMethod(methodIds[which]);
                Toast.makeText(this, "Method updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                // Reload prayer times with new method
                loadPrayerTimes();
            })
            .setNegativeButton("Cancel", null)
            .show();
}

// -----------------------------------------------------
// APPEARANCE (already existed, keep it)
// -----------------------------------------------------

private void showAppearanceDialog() {
    String[] themes = {"System default", "Light", "Dark"};
    int current = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    int checked = 0;
    if (current == AppCompatDelegate.MODE_NIGHT_NO) checked = 1;
    else if (current == AppCompatDelegate.MODE_NIGHT_YES) checked = 2;

    new MaterialAlertDialogBuilder(this)
            .setTitle("Appearance")
            .setSingleChoiceItems(themes, checked, (dialog, which) -> {
                int mode;
                switch (which) {
                    case 1:  mode = AppCompatDelegate.MODE_NIGHT_NO; break;
                    case 2:  mode = AppCompatDelegate.MODE_NIGHT_YES; break;
                    default: mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                }
                getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
                        .edit().putInt(KEY_THEME_MODE, mode).apply();
                AppCompatDelegate.setDefaultNightMode(mode);
                dialog.dismiss();
                recreate(); // apply theme immediately
            })
            .setNegativeButton("Cancel", null)
            .show();
}

    private void bindViews() {
        prayerLocation   = findViewById(R.id.prayerLocation);
        currentPrayerTime = findViewById(R.id.currentPrayerTime);
        currentPrayerName = findViewById(R.id.currentPrayerName);
        remainingTime    = findViewById(R.id.remainingTime);

        fajrTime    = findViewById(R.id.fajrTime);
        dhuhrTime   = findViewById(R.id.dhuhrTime);
        asrTime     = findViewById(R.id.asrTime);
        maghribTime = findViewById(R.id.maghribTime);
        ishaTime    = findViewById(R.id.ishaTime);
        
        fajrCard    = findViewById(R.id.fajrCard);
        dhuhrCard   = findViewById(R.id.dhuhrCard);
        asrCard     = findViewById(R.id.asrCard);
        maghribCard = findViewById(R.id.maghribCard);
        ishaCard    = findViewById(R.id.ishaCard);

        if (prayerLocation != null) {
    prayerLocation.setOnClickListener(v -> showLocationDialog());
}
    }

    private void loadPrayerTimes() {
        if (prefs.isUseGps()) {
            requestLocationAndFetch();
        } else {
            fetchByCity();
        }
    }

    private void requestLocationAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        getLocationAndFetch();
    }

    private void getLocationAndFetch() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) {
            fetchByCity();
            return;
        }

        try {
            Location last = null;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last == null) {
                    last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }

            if (last != null) {
                prefs.saveLastLocation(last.getLatitude(), last.getLongitude());
                fetchByLocation(last.getLatitude(), last.getLongitude());
            } else {
                // Request a single update
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        prefs.saveLastLocation(location.getLatitude(), location.getLongitude());
                        fetchByLocation(location.getLatitude(), location.getLongitude());
                    }
                    @Override public void onProviderEnabled(@NonNull String provider) {}
                    @Override public void onProviderDisabled(@NonNull String provider) {
                        fetchByCity();
                    }
                }, Looper.getMainLooper());

                // Fallback after 8 seconds
                handler.postDelayed(this::fetchByCity, 8000);
            }
        } catch (SecurityException e) {
            fetchByCity();
        }
    }

    private void fetchByLocation(double lat, double lng) {
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        int method = prefs.getMethod();

        PrayerTimesApi.getTimingsByLocation(lat, lng, method, date, new PrayerTimesApi.Callback() {
            @Override
            public void onSuccess(PrayerResponse response) {
                runOnUiThread(() -> updateUI(response));
            }
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "GPS failed, using city…", Toast.LENGTH_SHORT).show();
                    fetchByCity();
                });
            }
        });
    }

    private void fetchByCity() {
        String city = prefs.getCity();
        String country = prefs.getCountry();
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        int method = prefs.getMethod();

        PrayerTimesApi.getTimingsByCity(city, country, method, date, new PrayerTimesApi.Callback() {
            @Override
            public void onSuccess(PrayerResponse response) {
                runOnUiThread(() -> updateUI(response));
            }
            @Override
public void onError(String message) {
    runOnUiThread(() -> {

        // Use the selected city's saved coordinates.
        calculatePrayerTimesOffline(
                prefs.getLastLat(),
                prefs.getLastLng(),
                prefs.getTimeZone()
        );

    });

}
        });
    }
    
    private void calculatePrayerTimesOffline(
        double latitude,
        double longitude,
        String timeZoneId) {

    try {
        ZoneId zoneId;

        try {
            zoneId = ZoneId.of(timeZoneId);
        } catch (DateTimeException | NullPointerException e) {
            zoneId = ZoneId.systemDefault();
        }

        LocalDate today = LocalDate.now(zoneId);

        // Standard Asr rule: factor 1.
        // Factor 2 is used for the Hanafi rule.
        int asrFactor = 1;

        PrayerTimings timings = PrayerCalculationEngine.calculate(
                today,
                latitude,
                longitude,
                zoneId,
                prefs.getMethod(),
                asrFactor
        );

        currentTimings = timings;

        // Update the existing prayer cards.
        if (fajrTime != null) {
            fajrTime.setText(cleanTime(timings.Fajr));
        }

        if (dhuhrTime != null) {
            dhuhrTime.setText(cleanTime(timings.Dhuhr));
        }

        if (asrTime != null) {
            asrTime.setText(cleanTime(timings.Asr));
        }

        if (maghribTime != null) {
            maghribTime.setText(cleanTime(timings.Maghrib));
        }

        if (ishaTime != null) {
            ishaTime.setText(cleanTime(timings.Isha));
        }

        if (prayerLocation != null) {
            String location = prefs.getCity() + ", "
                    + prefs.getCountry();

            prayerLocation.setText(location + " · Offline");
        }

        updateNextPrayer();
        startRemainingTimeTicker();

    } catch (Exception e) {
        Toast.makeText(
                this,
                "Could not calculate prayer times offline.",
                Toast.LENGTH_LONG
        ).show();
    }
}
    
    private boolean showCachedPrayerTimes() {
    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    PrayerTimings cached = prefs.getCachedTimings(prefs.getCity(), prefs.getCountry(), today, prefs.getMethod());
    if (cached == null) return false;

    currentTimings = cached;
    if (prayerLocation != null) {
        prayerLocation.setText(prefs.getCity() + ", " + prefs.getCountry() + " · Cached");
    }
    if (fajrTime != null) fajrTime.setText(cleanTime(currentTimings.Fajr));
    if (dhuhrTime != null) dhuhrTime.setText(cleanTime(currentTimings.Dhuhr));
    if (asrTime != null) asrTime.setText(cleanTime(currentTimings.Asr));
    if (maghribTime != null) maghribTime.setText(cleanTime(currentTimings.Maghrib));
    if (ishaTime != null) ishaTime.setText(cleanTime(currentTimings.Isha));
    updateNextPrayer();
    startRemainingTimeTicker();
    Toast.makeText(this, "Offline: showing today's saved prayer times",
            Toast.LENGTH_SHORT).show();
    return true;
}

    private void updateUI(PrayerResponse response) {
        if (response == null || response.data == null || response.data.timings == null) return;

        currentTimings = response.data.timings;
        
        if (!prefs.isUseGps()) {
    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    prefs.saveCachedTimings(currentTimings, prefs.getCity(), prefs.getCountry(), today, prefs.getMethod());
}

    // Location text

    if (prefs.isUseGps()) {
      resolveLocationName(prefs.getLastLat(), prefs.getLastLng());
    } else {
      if (prayerLocation != null) {
        prayerLocation.setText(prefs.getCity() + ", " + prefs.getCountry());
      }
    }

        // Fill prayer list
        if (fajrTime != null)    fajrTime.setText(cleanTime(currentTimings.Fajr));
        if (dhuhrTime != null)   dhuhrTime.setText(cleanTime(currentTimings.Dhuhr));
        if (asrTime != null)     asrTime.setText(cleanTime(currentTimings.Asr));
        if (maghribTime != null) maghribTime.setText(cleanTime(currentTimings.Maghrib));
        if (ishaTime != null)    ishaTime.setText(cleanTime(currentTimings.Isha));

        // Next prayer + remaining time
        updateNextPrayer();
        startRemainingTimeTicker();
    }

    private String cleanTime(String t) {
        if (t == null) return "--:--";
        // Remove timezone suffix if present (e.g. "18:36 (BST)")
        return t.split(" ")[0];
    }
    
private void updateNextPrayer() {
    if (currentTimings == null) return;

    String[] names = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
    String[] times = {
            cleanTime(currentTimings.Fajr),
            cleanTime(currentTimings.Dhuhr),
            cleanTime(currentTimings.Asr),
            cleanTime(currentTimings.Maghrib),
            cleanTime(currentTimings.Isha)
    };

    com.google.android.material.card.MaterialCardView[] cards = {
            fajrCard, dhuhrCard, asrCard, maghribCard, ishaCard
    };

    Calendar now = Calendar.getInstance();
    int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

    String nextName = names[0];
    String nextTime = times[0];
    int nextMinutes = parseMinutes(times[0]) + 24 * 60; // tomorrow Fajr as fallback
    int nextIndex = 0;

    for (int i = 0; i < times.length; i++) {
        int t = parseMinutes(times[i]);
        if (t > nowMinutes) {
            nextName = names[i];
            nextTime = times[i];
            nextMinutes = t;
            nextIndex = i;
            break;
        }
    }

    // Update big hero section
    if (currentPrayerName != null) currentPrayerName.setText(nextName);
    if (currentPrayerTime != null) currentPrayerTime.setText(nextTime);

    // Remaining time
    int remaining = nextMinutes - nowMinutes;
    if (remaining < 0) remaining += 24 * 60;
    updateRemainingText(remaining);

    // ===== Highlight the next prayer card =====
    highlightNextPrayerCard(nextIndex, cards);
}

private void highlightNextPrayerCard(int nextIndex,
                                     com.google.android.material.card.MaterialCardView[] cards) {

    int highlightColor = 0xFFD4A017; // gold/yellow
    int transparent = 0x00000000;

    for (int i = 0; i < cards.length; i++) {
        if (cards[i] == null) continue;

        if (i == nextIndex) {
            // Highlight this card
            cards[i].setStrokeWidth(4);               // 4dp border
            cards[i].setStrokeColor(highlightColor);
        } else {
            // Remove highlight
            cards[i].setStrokeWidth(0);
            cards[i].setStrokeColor(transparent);
        }
    }
}

    private int parseMinutes(String hhmm) {
        try {
            String[] p = hhmm.split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private void updateRemainingText(int totalMinutes) {
        if (remainingTime == null) return;
        long hours = totalMinutes / 60;
        long mins = totalMinutes % 60;
        remainingTime.setText(String.format(Locale.getDefault(),
                "Remaining Time: %d:%02d:00", hours, mins));
    }

    private void startRemainingTimeTicker() {
        if (remainingTimeUpdater != null) handler.removeCallbacks(remainingTimeUpdater);
        remainingTimeUpdater = new Runnable() {
            @Override
            public void run() {
                updateNextPrayer();
                handler.postDelayed(this, 30_000); // every 30 sec
            }
        };
        handler.postDelayed(remainingTimeUpdater, 30_000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndFetch();
            } else {
                prefs.setUseGps(false);
                fetchByCity();
            }
        }
    }
    
    private void resolveLocationName(double lat, double lng) {
    new Thread(() -> {
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(this, Locale.getDefault());
            java.util.List<android.location.Address> addresses = geocoder.getFromLocation(lat, lng, 1);

            String displayName;
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address addr = addresses.get(0);
                String city = addr.getLocality();
                if (city == null) city = addr.getSubAdminArea();
                if (city == null) city = addr.getAdminArea();
                String country = addr.getCountryName();

                if (city != null && country != null) {
                    displayName = city + ", " + country;
                } else if (country != null) {
                    displayName = country;
                } else {
                    displayName = String.format(Locale.US, "%.2f, %.2f", lat, lng);
                }
            } else {
                displayName = String.format(Locale.US, "%.2f, %.2f", lat, lng);
            }

            String finalName = displayName;
            runOnUiThread(() -> {
                if (prayerLocation != null) {
                    prayerLocation.setText(finalName);
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                if (prayerLocation != null) {
                    prayerLocation.setText(String.format(Locale.US, "%.2f, %.2f", lat, lng));
                }
            });
        }
    }).start();
}

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh if user changed settings in ReminderActivity
        if (prefs != null) loadPrayerTimes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (remainingTimeUpdater != null) handler.removeCallbacks(remainingTimeUpdater);
    }

    // ---------- Theme (already in your code) ----------
    private void applySavedTheme() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        int savedMode = preferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }

    
    private void updatePrayerDate() {
        TextView prayerDate = findViewById(R.id.prayerDate);
        if (prayerDate != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
            prayerDate.setText(formatter.format(new Date()));
        }
    }

    private void setupFeatureGrid() {
        RecyclerView recycler = findViewById(R.id.featuresRecycler);
        if (recycler == null) return;

        recycler.setNestedScrollingEnabled(false);
        recycler.setLayoutManager(new GridLayoutManager(this, 3));

        ArrayList<Feature> features = new ArrayList<>();
        features.add(new Feature("Reminder", R.drawable.ic_schedule_24, R.drawable.bg_purple_circle));
        features.add(new Feature("Memorize", R.drawable.ic_lightbulb_24, R.drawable.bg_yellow_circle));
        features.add(new Feature("Ruqiyah", R.drawable.ic_science_24, R.drawable.bg_violet_circle));
        features.add(new Feature("Dua Q&A", R.drawable.ic_chat_bubble_24, R.drawable.bg_blue_circle));
        features.add(new Feature("Books", R.drawable.ic_menu_book_24, R.drawable.bg_green_circle));
        features.add(new Feature("Donate", R.drawable.ic_volunteer_activism_24, R.drawable.bg_red_circle));

        FeatureAdapter adapter = new FeatureAdapter(features, feature -> {
            switch (feature.getTitle()) {
                case "Reminder":
                    startActivity(new Intent(this, ReminderActivity.class));
                    break;
                case "Memorize":
                    startActivity(new Intent(this, MemorizeActivity.class));
                    break;
                case "Ruqiyah":
                    startActivity(new Intent(this, RuqiyahActivity.class));
                    break;
                case "Dua Q&A":
                    startActivity(new Intent(this, DuaActivity.class));
                    break;
                case "Books":
                    startActivity(new Intent(this, BooksActivity.class));
                    break;
                case "Donate":
                    startActivity(new Intent(this, DonateActivity.class));
                    break;
            }
        });
        recycler.setAdapter(adapter);
    }
}
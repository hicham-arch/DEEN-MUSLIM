package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 2002;

    private PrayerPrefs prefs;
    private TextView reminderLocation, reminderMethod;
    private TextView reminderNextName, reminderNextTime, reminderRemaining;
    private TextView rFajr, rDhuhr, rAsr, rMaghrib, rIsha;

    private PrayerTimings currentTimings;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;

    private final String[] METHOD_NAMES = {
            "University of Islamic Sciences, Karachi",
            "Islamic Society of North America (ISNA)",
            "Muslim World League",
            "Umm Al-Qura University, Makkah",
            "Egyptian General Authority of Survey",
            "Institute of Geophysics, University of Tehran",
            "Gulf Region",
            "Kuwait",
            "Qatar",
            "Majlis Ugama Islam Singapura, Singapore",
            "Union Organization islamic de France",
            "Diyanet İşleri Başkanlığı, Turkey",
            "Spiritual Administration of Muslims of Russia",
            "Moonsighting Committee Worldwide"
    };
    // IDs: 1,2,3,4,5,7,8,9,10,11,12,13,14,15
    private final int[] METHOD_IDS = {1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        prefs = new PrayerPrefs(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        setupButtons();
        loadPrayerTimes();
    }

    private void bindViews() {
        reminderLocation  = findViewById(R.id.reminderLocation);
        reminderMethod    = findViewById(R.id.reminderMethod);
        reminderNextName  = findViewById(R.id.reminderNextName);
        reminderNextTime  = findViewById(R.id.reminderNextTime);
        reminderRemaining = findViewById(R.id.reminderRemaining);

        rFajr    = findViewById(R.id.rFajr);
        rDhuhr   = findViewById(R.id.rDhuhr);
        rAsr     = findViewById(R.id.rAsr);
        rMaghrib = findViewById(R.id.rMaghrib);
        rIsha    = findViewById(R.id.rIsha);
    }

    private void setupButtons() {
        findViewById(R.id.btnChangeCity).setOnClickListener(v -> showCityDialog());
        findViewById(R.id.btnUseGps).setOnClickListener(v -> {
            prefs.setUseGps(true);
            requestLocationAndFetch();
        });
        findViewById(R.id.btnChangeMethod).setOnClickListener(v -> showMethodDialog());
    }

    private void loadPrayerTimes() {
        updateMethodLabel();
        if (prefs.isUseGps()) {
            requestLocationAndFetch();
        } else {
            fetchByCity();
        }
    }

    private void updateMethodLabel() {
        int method = prefs.getMethod();
        String name = "Method " + method;
        for (int i = 0; i < METHOD_IDS.length; i++) {
            if (METHOD_IDS[i] == method) {
                name = METHOD_NAMES[i];
                break;
            }
        }
        reminderMethod.setText("Method: " + name);
    }

    private void showCityDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        final EditText cityInput = new EditText(this);
        cityInput.setHint("City");
        cityInput.setText(prefs.getCity());
        cityInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(cityInput);

        final EditText countryInput = new EditText(this);
        countryInput.setHint("Country");
        countryInput.setText(prefs.getCountry());
        countryInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(countryInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Set Location")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String city = cityInput.getText().toString().trim();
                    String country = countryInput.getText().toString().trim();
                    if (!city.isEmpty() && !country.isEmpty()) {
                        prefs.setCity(city);
                        prefs.setCountry(country);
                        prefs.setUseGps(false);
                        fetchByCity();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMethodDialog() {
        int current = prefs.getMethod();
        int selected = 1; // default ISNA index
        for (int i = 0; i < METHOD_IDS.length; i++) {
            if (METHOD_IDS[i] == current) {
                selected = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Calculation Method")
                .setSingleChoiceItems(METHOD_NAMES, selected, (dialog, which) -> {
                    prefs.setMethod(METHOD_IDS[which]);
                    updateMethodLabel();
                    loadPrayerTimes();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestLocationAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        getLocationAndFetch();
    }

    private void getLocationAndFetch() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            Location last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (last == null) last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (last != null) {
                prefs.saveLastLocation(last.getLatitude(), last.getLongitude());
                fetchByLocation(last.getLatitude(), last.getLongitude());
            } else {
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        prefs.saveLastLocation(location.getLatitude(), location.getLongitude());
                        fetchByLocation(location.getLatitude(), location.getLongitude());
                    }
                    @Override public void onProviderEnabled(@NonNull String p) {}
                    @Override public void onProviderDisabled(@NonNull String p) {
                        fetchByCity();
                    }
                }, Looper.getMainLooper());
                handler.postDelayed(this::fetchByCity, 8000);
            }
        } catch (SecurityException e) {
            fetchByCity();
        }
    }

    private void fetchByLocation(double lat, double lng) {
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        PrayerTimesApi.getTimingsByLocation(lat, lng, prefs.getMethod(), date,
                new PrayerTimesApi.Callback() {
                    @Override
                    public void onSuccess(PrayerResponse response) {
                        runOnUiThread(() -> updateUI(response, true));
                    }
                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(ReminderActivity.this, "GPS error, using city", Toast.LENGTH_SHORT).show();
                            fetchByCity();
                        });
                    }
                });
    }

    private void fetchByCity() {
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        PrayerTimesApi.getTimingsByCity(prefs.getCity(), prefs.getCountry(),
                prefs.getMethod(), date, new PrayerTimesApi.Callback() {
                    @Override
                    public void onSuccess(PrayerResponse response) {
                        runOnUiThread(() -> updateUI(response, false));
                    }
                    @Override
                    public void onError(String message) {
                        runOnUiThread(() ->
                                Toast.makeText(ReminderActivity.this, "Error: " + message, Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void updateUI(PrayerResponse response, boolean fromGps) {
        if (response == null || response.data == null) return;
        currentTimings = response.data.timings;

        if (fromGps && response.data.meta != null) {
            reminderLocation.setText(String.format(Locale.US,
                    "GPS: %.4f, %.4f", response.data.meta.latitude, response.data.meta.longitude));
        } else {
            reminderLocation.setText(prefs.getCity() + ", " + prefs.getCountry());
        }

        rFajr.setText(clean(currentTimings.Fajr));
        rDhuhr.setText(clean(currentTimings.Dhuhr));
        rAsr.setText(clean(currentTimings.Asr));
        rMaghrib.setText(clean(currentTimings.Maghrib));
        rIsha.setText(clean(currentTimings.Isha));

        updateNextPrayer();
        startTicker();
    }

    private String clean(String t) {
        return t == null ? "--:--" : t.split(" ")[0];
    }

    private void updateNextPrayer() {
        if (currentTimings == null) return;

        String[] names = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
        String[] times = {
                clean(currentTimings.Fajr), clean(currentTimings.Dhuhr),
                clean(currentTimings.Asr), clean(currentTimings.Maghrib),
                clean(currentTimings.Isha)
        };

        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        String nextName = names[0];
        String nextTime = times[0];
        int nextMin = parseMin(times[0]) + 24 * 60;

        for (int i = 0; i < 5; i++) {
            int t = parseMin(times[i]);
            if (t > nowMin) {
                nextName = names[i];
                nextTime = times[i];
                nextMin = t;
                break;
            }
        }

        reminderNextName.setText(nextName);
        reminderNextTime.setText(nextTime);

        int rem = nextMin - nowMin;
        if (rem < 0) rem += 24 * 60;
        long h = rem / 60;
        long m = rem % 60;
        reminderRemaining.setText(String.format(Locale.getDefault(), "Remaining: %d h %02d m", h, m));
    }

    private int parseMin(String hhmm) {
        try {
            String[] p = hhmm.split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private void startTicker() {
        if (ticker != null) handler.removeCallbacks(ticker);
        ticker = () -> {
            updateNextPrayer();
            handler.postDelayed(ticker, 30_000);
        };
        handler.postDelayed(ticker, 30_000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ticker != null) handler.removeCallbacks(ticker);
    }
}
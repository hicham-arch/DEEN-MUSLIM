package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCES_NAME = "deen_muslim_settings";
    private static final String KEY_THEME_MODE = "theme_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply the saved mode before inflating the views, avoiding a light-theme flash.
        applySavedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updatePrayerDate();
        setupFeatureGrid();

        findViewById(R.id.appearanceButton).setOnClickListener(v -> showAppearanceDialog());
    }

    private void applySavedTheme() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        int savedMode = preferences.getInt(
                KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }

    /** Opens a persistent Light / Dark / System appearance selector. */
    private void showAppearanceDialog() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        int currentMode = preferences.getInt(
                KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        final int[] modes = {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                AppCompatDelegate.MODE_NIGHT_NO,
                AppCompatDelegate.MODE_NIGHT_YES
        };
        String[] labels = {"System default", "Light", "Dark"};
        int selected = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == currentMode) {
                selected = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Appearance")
                .setMessage("Choose how DEEN MUSLIM looks")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    int mode = modes[which];
                    preferences.edit().putInt(KEY_THEME_MODE, mode).apply();
                    AppCompatDelegate.setDefaultNightMode(mode);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Keeps the displayed date current instead of leaving a sample date in the layout. */
    private void updatePrayerDate() {
        TextView prayerDate = findViewById(R.id.prayerDate);
        if (prayerDate != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
            prayerDate.setText(formatter.format(new Date()));
        }
    }

    private void setupFeatureGrid() {
        RecyclerView recycler = findViewById(R.id.featuresRecycler);
        if (recycler == null) {
            return;
        }

        // The grid is inside the page ScrollView; let the page handle scrolling.
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
                default:
                    // Ignore unknown actions safely.
                    break;
            }
        });

        recycler.setAdapter(adapter);
        recycler.setVisibility(android.view.View.VISIBLE);
    }
}

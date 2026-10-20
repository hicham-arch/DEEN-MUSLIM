package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    updatePrayerDate();
    setupFeatureGrid();
  }

  /** Keeps the date label current instead of leaving the old sample date in the layout. */
  private void updatePrayerDate() {
    TextView prayerDate = findViewById(R.id.prayerDate);
    if (prayerDate != null) {
      SimpleDateFormat formatter = new SimpleDateFormat("d MMMM, yyyy", Locale.getDefault());
      prayerDate.setText(formatter.format(new Date()));
    }
  }

  private void setupFeatureGrid() {
    RecyclerView recycler = findViewById(R.id.featuresRecycler);
    if (recycler == null) {
      return;
    }

    // The RecyclerView sits inside the page ScrollView, so it must not scroll independently.
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

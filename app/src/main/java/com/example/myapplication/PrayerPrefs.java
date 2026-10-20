package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class PrayerPrefs {

    private static final String PREF_NAME = "prayer_prefs";
    private static final String KEY_METHOD = "calc_method";
    private static final String KEY_CITY = "city";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_USE_GPS = "use_gps";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";

    private final SharedPreferences prefs;

    public PrayerPrefs(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getMethod() {
        return prefs.getInt(KEY_METHOD, 2); // default ISNA
    }

    public void setMethod(int method) {
        prefs.edit().putInt(KEY_METHOD, method).apply();
    }

    public String getCity() {
        return prefs.getString(KEY_CITY, "Sylhet");
    }

    public void setCity(String city) {
        prefs.edit().putString(KEY_CITY, city).apply();
    }

    public String getCountry() {
        return prefs.getString(KEY_COUNTRY, "Bangladesh");
    }

    public void setCountry(String country) {
        prefs.edit().putString(KEY_COUNTRY, country).apply();
    }

    public boolean isUseGps() {
        return prefs.getBoolean(KEY_USE_GPS, true);
    }

    public void setUseGps(boolean use) {
        prefs.edit().putBoolean(KEY_USE_GPS, use).apply();
    }

    public void saveLastLocation(double lat, double lng) {
        prefs.edit()
                .putString(KEY_LAT, String.valueOf(lat))
                .putString(KEY_LNG, String.valueOf(lng))
                .apply();
    }

    public double getLastLat() {
        return Double.parseDouble(prefs.getString(KEY_LAT, "24.8949"));
    }

    public double getLastLng() {
        return Double.parseDouble(prefs.getString(KEY_LNG, "91.8687"));
    }
}
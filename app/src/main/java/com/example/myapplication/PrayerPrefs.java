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
    private static final String KEY_ASR_METHOD = "asr_method";


    private static final String KEY_CACHE_CITY = "cached_prayer_city";
    private static final String KEY_CACHE_COUNTRY = "cached_prayer_country";
    private static final String KEY_CACHE_DATE = "cached_prayer_date";
    private static final String KEY_CACHE_METHOD = "cached_prayer_method";
    private static final String KEY_CACHE_FAJR = "cached_prayer_fajr";
    private static final String KEY_CACHE_SUNRISE = "cached_prayer_sunrise";
    private static final String KEY_CACHE_DHUHR = "cached_prayer_dhuhr";
    private static final String KEY_CACHE_ASR = "cached_prayer_asr";
    private static final String KEY_CACHE_MAGHRIB = "cached_prayer_maghrib";
    private static final String KEY_CACHE_ISHA = "cached_prayer_isha";
    private static final String KEY_TIMEZONE = "location_timezone";
    private static final String KEY_CACHE_TIMEZONE = "cached_prayer_timezone";
    private static final String KEY_CACHE_LAT = "cached_prayer_lat";
    private static final String KEY_CACHE_LNG = "cached_prayer_lng";

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

    public int getAsrFactor() {
        return prefs.getInt(KEY_ASR_METHOD, 1); // Default 1 = Standard
    }

    public void setAsrFactor(int factor) {
        prefs.edit().putInt(KEY_ASR_METHOD, factor).apply();
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
    

public void setTimeZone(String timezone) {
    prefs.edit()
            .putString(KEY_TIMEZONE, timezone)
            .apply();
}

public String getTimeZone() {
    return prefs.getString(
            KEY_TIMEZONE,
            java.util.TimeZone.getDefault().getID()
    );
}

    /** Save today's timings for the currently selected manual city. */
    public void saveCachedTimings(PrayerTimings timings, String city, String country, String date, int method,
                                 String timezone, int asrFactor, double latitude, double longitude) {
        if (timings == null) return;
        prefs.edit()
                .putString(KEY_CACHE_CITY, city)
                .putString(KEY_CACHE_COUNTRY, country)
                .putString(KEY_CACHE_DATE, date)
                .putInt(KEY_CACHE_METHOD, method)
                .putString(KEY_CACHE_TIMEZONE, timezone)
                .putInt("cached_prayer_asr_factor", asrFactor)
                .putString(KEY_CACHE_LAT, String.valueOf(latitude))
                .putString(KEY_CACHE_LNG, String.valueOf(longitude))
                .putString(KEY_CACHE_FAJR, timings.Fajr)
                .putString(KEY_CACHE_SUNRISE, timings.Sunrise)
                .putString(KEY_CACHE_DHUHR, timings.Dhuhr)
                .putString(KEY_CACHE_ASR, timings.Asr)
                .putString(KEY_CACHE_MAGHRIB, timings.Maghrib)
                .putString(KEY_CACHE_ISHA, timings.Isha)
                .apply();
    }


    /**
     * Returns cached timings only for the same city/country and local date.
     * Yesterday's timings are deliberately not shown as today's prayer schedule.
     */
    public PrayerTimings getCachedTimings(String city, String country, String date, int method, String timezone,
                                         int asrFactor, double latitude, double longitude) {
        if (!city.equals(prefs.getString(KEY_CACHE_CITY, ""))
                || !country.equals(prefs.getString(KEY_CACHE_COUNTRY, ""))
                || !date.equals(prefs.getString(KEY_CACHE_DATE, ""))
                || method != prefs.getInt(KEY_CACHE_METHOD, -1)
                || !timezone.equals(prefs.getString(KEY_CACHE_TIMEZONE, ""))
                || asrFactor != prefs.getInt("cached_prayer_asr_factor", -1)
                || Math.abs(latitude - Double.parseDouble(prefs.getString(KEY_CACHE_LAT, "0"))) > 1e-9
                || Math.abs(longitude - Double.parseDouble(prefs.getString(KEY_CACHE_LNG, "0"))) > 1e-9) {
            return null;
        }
        String fajr = prefs.getString(KEY_CACHE_FAJR, null);

        String dhuhr = prefs.getString(KEY_CACHE_DHUHR, null);
        String asr = prefs.getString(KEY_CACHE_ASR, null);
        String maghrib = prefs.getString(KEY_CACHE_MAGHRIB, null);
        String isha = prefs.getString(KEY_CACHE_ISHA, null);
        if (fajr == null || dhuhr == null || asr == null || maghrib == null || isha == null) {
            return null;
        }
        PrayerTimings timings = new PrayerTimings();
        timings.Fajr = fajr;
        timings.Sunrise = prefs.getString(KEY_CACHE_SUNRISE, null);
        timings.Dhuhr = dhuhr;
        timings.Asr = asr;
        timings.Maghrib = maghrib;
        timings.Isha = isha;
        return timings;
    }
}

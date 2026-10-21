package com.example.myapplication;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Offline solar prayer-time calculator. No network or third-party dependency required.
 * Angles/rules are configurable by calculation method; results are approximate and should
 * be checked against the user's local mosque timetable for religious practice.
 */
public final class PrayerCalculationEngine {
    private PrayerCalculationEngine() {}

    private static final double RAD = Math.PI / 180.0;
    private static final double DEG = 180.0 / Math.PI;

    private static final class Params {
        double fajrAngle;
        double ishaAngle;
        double ishaMinutes;
        int asrFactor = 1;
        double maghribAngle = 0.833;
        Params(double f, double i) { fajrAngle = f; ishaAngle = i; }
    }

    /** Aladhan-style method IDs: 1 Karachi, 2 ISNA, 3 MWL, 4 Umm al-Qura, 5 Egypt,
     * 7 Tehran, 8 Gulf, 9 Kuwait, 10 Qatar, 11 Singapore, 12 France, 13 Turkey, 14 Russia. */
    private static Params paramsFor(int method) {
        switch (method) {
            case 1: return new Params(18.0, 18.0);
            case 2: return new Params(15.0, 15.0);
            case 3: return new Params(18.0, 17.0);
            case 4: { Params p = new Params(18.5, 0); p.ishaMinutes = 90; return p; }
            case 5: return new Params(19.5, 17.5);
            case 7: return new Params(17.7, 14.0);
            case 8: { Params p = new Params(19.5, 0); p.ishaMinutes = 90; return p; }
            case 9: return new Params(18.0, 17.5);
            case 10: { Params p = new Params(18.0, 0); p.ishaMinutes = 90; return p; }
            case 11: return new Params(20.0, 18.0);
            case 12: return new Params(12.0, 12.0);
            case 13: { Params p = new Params(18.0, 17.0); p.ishaMinutes = 0; return p; }
            case 14: return new Params(16.0, 15.0);
            default: return new Params(18.0, 17.0); // MWL fallback
        }
    }

    /**
     * @param date local calendar date at the selected location
     * @param latitude latitude in degrees (-90..90)
     * @param longitude longitude in degrees (-180..180; east positive)
     * @param zoneId IANA time zone, e.g. Africa/Casablanca
     * @param method Aladhan-style calculation method ID
     * @param asrFactor 1 for Shafi'i/standard, 2 for Hanafi
     */
    public static PrayerTimings calculate(LocalDate date, double latitude, double longitude,
                                          ZoneId zoneId, int method, int asrFactor) {
        if (date == null || zoneId == null || !Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Valid date, time zone, latitude and longitude are required.");
        }
        Params p = paramsFor(method);
        p.asrFactor = asrFactor == 2 ? 2 : 1;
        double day = ChronoUnit.DAYS.between(LocalDate.of(date.getYear(), 1, 1), date) + 1;
        double gamma = 2.0 * Math.PI / 365.0 * (day - 1);
        double eqTime = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma)
                - 0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma));
        double decl = 0.006918 - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma)
                - 0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma)
                - 0.002697 * Math.cos(3 * gamma) + 0.00148 * Math.sin(3 * gamma);

        ZonedDateTime noonZdt = date.atTime(12, 0).atZone(zoneId);
        double tz = noonZdt.getOffset().getTotalSeconds() / 3600.0;
        double solarNoon = 12.0 + tz - longitude / 15.0 - eqTime / 60.0;
        double sunriseHourAngle = hourAngle(latitude, decl, p.maghribAngle);
        double fajrHourAngle = hourAngle(latitude, decl, p.fajrAngle);
        double ishaHourAngle = p.ishaMinutes > 0 ? Double.NaN : hourAngle(latitude, decl, p.ishaAngle);
        double asrAltitude = -Math.atan(1.0 / (p.asrFactor + Math.tan(Math.abs(latitude * RAD - decl)))) * DEG;
        double asrHourAngle = hourAngleFromAltitude(latitude, decl, asrAltitude);

        double sunrise = solarNoon - sunriseHourAngle / 15.0;
        double sunset = solarNoon + sunriseHourAngle / 15.0;
        double fajr = solarNoon - fajrHourAngle / 15.0;
        double asr = solarNoon + asrHourAngle / 15.0;
        double isha = p.ishaMinutes > 0 ? sunset + p.ishaMinutes / 60.0 : solarNoon + ishaHourAngle / 15.0;

        // High-latitude fallback: cap unavailable twilight times using one-seventh of night.
        double night = (24.0 - sunset) + sunrise;
        if (!Double.isFinite(fajr)) fajr = sunrise - night / 7.0;
        if (!Double.isFinite(isha)) isha = sunset + night / 7.0;
        if (fajr < 0 || fajr > sunrise) fajr = sunrise - night / 7.0;
        if (isha < sunset || isha > 24) isha = sunset + night / 7.0;

        PrayerTimings t = new PrayerTimings();
        t.Fajr = format(fajr);
        t.Sunrise = format(sunrise);
        t.Dhuhr = format(solarNoon);
        t.Asr = format(asr);
        t.Maghrib = format(sunset);
        t.Isha = format(isha);
        t.Imsak = format(fajr - 10.0 / 60.0);
        t.Midnight = format((sunset + (fajr + 24.0)) / 2.0);
        return t;
    }

    private static double hourAngle(double lat, double decl, double angle) {
        return hourAngleFromAltitude(lat, decl, -Math.abs(angle));
    }

    private static double hourAngleFromAltitude(double lat, double decl, double altitude) {
        double phi = lat * RAD;
        double h = altitude * RAD;
        double cosH = (Math.sin(h) - Math.sin(phi) * Math.sin(decl)) / (Math.cos(phi) * Math.cos(decl));
        if (cosH < -1 || cosH > 1 || !Double.isFinite(cosH)) return Double.NaN;
        return Math.acos(cosH) * DEG;
    }

    private static String format(double hours) {
        if (!Double.isFinite(hours)) return "--:--";
        int minutes = (int) Math.round(hours * 60.0);
        minutes = ((minutes % 1440) + 1440) % 1440;
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }
}

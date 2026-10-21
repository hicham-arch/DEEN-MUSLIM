package com.example.myapplication;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * High-precision offline solar prayer-time calculator.
 *
 * Implements the usual solar-position series and provides explicit high-latitude
 * fallbacks.  Calculations are performed for the supplied {@link LocalDate} in
 * the supplied {@link ZoneId}; time-zone conversion is the caller's
 * responsibility.
 */
public final class PrayerCalculationEngine {
    private PrayerCalculationEngine() {}

    /** No adjustment when the sun does not reach a required altitude. */
    public static final int HIGH_LATITUDE_NONE = 0;
    /** Fajr/Isha are placed half-way through the night. */
    public static final int HIGH_LATITUDE_MIDDLE_OF_NIGHT = 1;
    /** Fajr/Isha are one seventh of the night from sunrise/sunset. */
    public static final int HIGH_LATITUDE_ONE_SEVENTH = 2;
    /** Angle-based adjustment; currently falls back to one-seventh when needed. */
    public static final int HIGH_LATITUDE_ANGLE_BASED = 3;

    private static final double RAD = Math.PI / 180.0;
    private static final double DEG = 180.0 / Math.PI;

    private static final class Params {
        double fajrAngle;
        double ishaAngle;
        double ishaMinutes;
        double maghribMinutes = 0.0;
        int asrFactor = 1;
        double maghribAngle = 0.833; // standard refraction-adjusted sunrise/sunset altitude
        Params(double f, double i) { fajrAngle = f; ishaAngle = i; }
    }

    /**
     * Mapping of the calculation-method IDs used by the UI and preferences to the
     * angles/parameters used here.  IDs are intentionally kept stable because
     * existing preferences may already contain saved IDs.
     */
    private static Params paramsFor(int method) {
        switch (method) {
            case 1:  return new Params(18.0, 18.0);                          // Karachi
            case 2:  return new Params(15.0, 15.0);                          // ISNA
            case 3:  return new Params(18.0, 17.0);                          // Muslim World League
            case 4:  { Params p = new Params(18.5, 0); p.ishaMinutes = 90; return p; } // Umm Al-Qura (Isha: +90 min)
            case 5:  return new Params(19.5, 17.5);                          // Egyptian
            case 7:  { Params p = new Params(17.7, 14.0); p.maghribAngle = 4.5; return p; } // Tehran
            case 8:  { Params p = new Params(19.5, 0); p.ishaMinutes = 90; return p; } // Gulf (Isha: +90 min)
            case 9:  return new Params(18.0, 17.5);                          // Kuwait
            case 10: { Params p = new Params(18.0, 0); p.ishaMinutes = 90; return p; } // Qatar (Isha: +90 min)
            case 11: return new Params(20.0, 18.0);                          // Singapore
            case 12: return new Params(12.0, 12.0);                          // France
            case 13: { Params p = new Params(18.0, 17.0); p.ishaMinutes = 0; return p; } // Turkey/Diyanet
            case 14: return new Params(16.0, 15.0);                          // Russia
            default: return new Params(18.0, 17.0);                          // fallback close to MWL
        }
    }

    public static PrayerTimings calculate(LocalDate date, double latitude, double longitude,
                                          ZoneId zoneId, int method, int asrFactor) {
        return calculate(date, latitude, longitude, zoneId, method, asrFactor,
                HIGH_LATITUDE_ONE_SEVENTH);
    }

    public static PrayerTimings calculate(LocalDate date, double latitude, double longitude,
                                          ZoneId zoneId, int method, int asrFactor,
                                          int highLatitudeRule) {
        if (date == null || zoneId == null || !Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Valid date, time zone, latitude and longitude are required.");
        }

        Params p = paramsFor(method);
        // The Asr method is independent of the calculation method.
        p.asrFactor = (asrFactor == 2) ? 2 : 1;

        // Day of year
        double day = ChronoUnit.DAYS.between(LocalDate.of(date.getYear(), 1, 1), date) + 1;

        // Solar coordinates
        double gamma = 2.0 * Math.PI / 365.2422 * (day - 1);

        // Equation of Time (minutes)
        double eqTime = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma)
                - 0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma));

        // Solar declination (radians)
        double decl = (0.006918 - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma)
                - 0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma)
                - 0.002697 * Math.cos(3 * gamma) + 0.00148 * Math.sin(3 * gamma));

        ZonedDateTime noonZdt = date.atTime(12, 0).atZone(zoneId);
        double tz = noonZdt.getOffset().getTotalSeconds() / 3600.0;

        // Solar noon (hours, local wall-clock of zoneId)
        double solarNoon = 12.0 + tz - longitude / 15.0 - eqTime / 60.0;

        // Hour angles
        double sunriseHourAngle = hourAngleFromAltitude(latitude, decl, -0.833);
        double maghribHourAngle = hourAngleFromAltitude(latitude, decl, -Math.abs(p.maghribAngle));
        double fajrHourAngle = hourAngleFromAltitude(latitude, decl, -Math.abs(p.fajrAngle));
        double ishaHourAngle = (p.ishaMinutes > 0) ? Double.NaN
                : hourAngleFromAltitude(latitude, decl, -Math.abs(p.ishaAngle));

        // Asr: the sun's altitude when the shadow length equals
        //   asrFactor (1=Standard/Shafi'i, 2=Hanafi) times the object height
        //   plus the shadow already present at solar noon.
        //   tan(altitude) = 1 / (asrFactor + |tan(latitude - declination)|)
        // The altitude is positive (the sun is above the horizon), so the sign is
        // positive here.  A negative atan() here would describe a point below the
        // horizon and is incorrect.
        double asrAltitude = Math.atan(1.0 / (p.asrFactor + Math.abs(Math.tan(latitude * RAD - decl)))) * DEG;
        double asrHourAngle = hourAngleFromAltitude(latitude, decl, asrAltitude);

        double sunrise = Double.isFinite(sunriseHourAngle) ? solarNoon - sunriseHourAngle / 15.0 : Double.NaN;
        double sunset = Double.isFinite(sunriseHourAngle) ? solarNoon + sunriseHourAngle / 15.0 : Double.NaN;
        double fajr = Double.isFinite(fajrHourAngle) ? solarNoon - fajrHourAngle / 15.0 : Double.NaN;
        double asr = Double.isFinite(asrHourAngle) ? solarNoon + asrHourAngle / 15.0 : Double.NaN;
        double isha = (p.ishaMinutes > 0)
                ? (Double.isFinite(sunset) ? sunset + p.ishaMinutes / 60.0 : Double.NaN)
                : (Double.isFinite(ishaHourAngle) ? solarNoon + ishaHourAngle / 15.0 : Double.NaN);

        // --- High-latitude handling ---
        // Night length used by the fallbacks.  When sunrise or sunset is
        // unavailable (polar day or polar night) there is no meaningful night
        // length and we must not synthesise one.
        double night = Double.NaN;
        if (Double.isFinite(sunrise) && Double.isFinite(sunset)) {
            night = (24.0 - sunset) + sunrise;
            if (!(night > 0.0 && night <= 24.0) || !Double.isFinite(night)) {
                night = Double.NaN;
            }
        }

        boolean needsFallback = !Double.isFinite(fajr) || !Double.isFinite(isha);
        if (needsFallback && Double.isFinite(night)) {
            switch (highLatitudeRule) {
                case HIGH_LATITUDE_NONE:
                    // No adjustment: leave the affected prayer unavailable.
                    break;
                case HIGH_LATITUDE_MIDDLE_OF_NIGHT:
                    if (!Double.isFinite(fajr)) fajr = sunrise - night / 2.0;
                    if (!Double.isFinite(isha)) isha = sunset + night / 2.0;
                    break;
                case HIGH_LATITUDE_ONE_SEVENTH:
                    if (!Double.isFinite(fajr)) fajr = sunrise - night / 7.0;
                    if (!Double.isFinite(isha)) isha = sunset + night / 7.0;
                    break;
                case HIGH_LATITUDE_ANGLE_BASED:
                    // Unsupported in this implementation. We intentionally do
                    // not present angle-based adjustment as a working option and
                    // leave the affected prayer unavailable.
                    break;
                default:
                    break;
            }
        }

        // Sanity handling must respect the selected high-latitude rule.
        if (highLatitudeRule == HIGH_LATITUDE_ONE_SEVENTH
                && Double.isFinite(sunrise) && Double.isFinite(sunset)) {
            if (Double.isFinite(fajr) && (fajr < 0 || fajr > sunrise)) {
                fajr = sunrise - night / 7.0;
            }
            if (Double.isFinite(isha) && (isha < sunset || isha > 24)) {
                isha = sunset + night / 7.0;
            }
        } else if (highLatitudeRule == HIGH_LATITUDE_MIDDLE_OF_NIGHT
                && Double.isFinite(sunrise) && Double.isFinite(sunset)) {
            if (Double.isFinite(fajr) && (fajr < 0 || fajr > sunrise)) {
                fajr = sunrise - night / 2.0;
            }
            if (Double.isFinite(isha) && (isha < sunset || isha > 24)) {
                isha = sunset + night / 2.0;
            }
        } else {
            // For NONE or unsupported angle-based, do not fabricate a replacement.
            if (!(highLatitudeRule == HIGH_LATITUDE_ONE_SEVENTH
                    || highLatitudeRule == HIGH_LATITUDE_MIDDLE_OF_NIGHT)) {
                if (Double.isFinite(fajr) && (fajr < 0 || fajr > sunrise)) {
                    fajr = Double.NaN;
                }
                if (Double.isFinite(isha) && (isha < sunset || isha > 24)) {
                    isha = Double.NaN;
                }
            }
        }

        // Prevent formatted output from turning an unavailable/negative result
        // into a plausible-looking wall-clock time.
        if (Double.isFinite(fajr) && (fajr < 0.0 || fajr >= 24.0)) fajr = Double.NaN;
        if (Double.isFinite(isha) && (isha < 0.0 || isha >= 24.0)) isha = Double.NaN;
        if (Double.isFinite(sunrise) && (sunrise < 0.0 || sunrise >= 24.0)) sunrise = Double.NaN;
        if (Double.isFinite(sunset) && (sunset < 0.0 || sunset >= 24.0)) sunset = Double.NaN;
        if (Double.isFinite(asr) && (asr < 0.0 || asr >= 24.0)) asr = Double.NaN;
        if (Double.isFinite(solarNoon) && (solarNoon < 0.0 || solarNoon >= 24.0)) solarNoon = Double.NaN;

        // High-latitude/polar cases may leave values non-finite: leave them
        // non-finite so callers render "--:--" instead of plausible junk.

        PrayerTimings t = new PrayerTimings();
        t.Fajr = format(fajr);
        t.Sunrise = format(sunrise);
        t.Dhuhr = format(solarNoon);
        t.Asr = format(asr);
        double maghrib = Double.isFinite(maghribHourAngle) ? solarNoon + maghribHourAngle / 15.0 : sunset;
        t.Maghrib = format(maghrib);
        t.Isha = format(isha);
        t.Imsak = Double.isFinite(fajr) ? format(fajr - 10.0 / 60.0) : "--:--";
        t.Midnight = (Double.isFinite(sunset) && Double.isFinite(fajr))
                ? format((sunset + (fajr + 24.0)) / 2.0) : "--:--";
        return t;
    }

    private static double hourAngleFromAltitude(double lat, double decl, double altitude) {
        double phi = lat * RAD;
        // decl is currently radians; convert consistently.
        double declRad = decl;
        // decl parameter passed may already be radians (from calculation). Keep as-is.
        double h = altitude * RAD;
        double cosPhi = Math.cos(phi);
        double cosDecl = Math.cos(declRad);
        if (Math.abs(cosPhi) < 1e-12 || Math.abs(cosDecl) < 1e-12) {
            return Double.NaN;
        }
        double cosH = (Math.sin(h) - Math.sin(phi) * Math.sin(declRad)) / (cosPhi * cosDecl);
        if (cosH < -1.0 || cosH > 1.0 || !Double.isFinite(cosH)) return Double.NaN;
        return Math.acos(cosH) * DEG;
    }

    private static String format(double hours) {
        if (!Double.isFinite(hours)) return "--:--";
        int minutes = (int) Math.round(hours * 60.0);
        minutes = ((minutes % 1440) + 1440) % 1440;
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }
}

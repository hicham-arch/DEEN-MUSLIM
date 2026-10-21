package com.example.myapplication;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PrayerCalculationEngineTest {
    private static final LocalDate DUBAI_DATE = LocalDate.of(2025, 6, 1);
    private static final ZoneId DUBAI_ZONE = ZoneId.of("Asia/Dubai");

    @Test
    public void standardAndHanafiAsrDifferAsExpected() {
        PrayerTimings standard = PrayerCalculationEngine.calculate(
                DUBAI_DATE, 25.2048, 55.2708, DUBAI_ZONE, 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_ONE_SEVENTH);
        PrayerTimings hanafi = PrayerCalculationEngine.calculate(
                DUBAI_DATE, 25.2048, 55.2708, DUBAI_ZONE, 2, 2,
                PrayerCalculationEngine.HIGH_LATITUDE_ONE_SEVENTH);
        assertEquals("15:40", standard.Asr);
        assertEquals("16:59", hanafi.Asr);
    }

    @Test
    public void noneRuleDoesNotFabricateMissingPrayer() {
        PrayerTimings result = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 6, 15), 69.6492, 18.9553,
                ZoneId.of("Europe/Oslo"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_NONE);
        assertEquals("--:--", result.Fajr);
        assertEquals("--:--", result.Isha);
    }

    @Test
    public void oneSeventhRuleIsUsedWhenAngleCannotBeReached() {
        PrayerTimings result = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 1, 15), 51.5074, -0.1278,
                ZoneId.of("Europe/London"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_ONE_SEVENTH);
        assertNotEquals("--:--", result.Fajr);
        assertNotEquals("--:--", result.Isha);
    }

    @Test
    public void angleBasedRuleRemainsExplicitlyUnavailable() {
        PrayerTimings result = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 6, 15), 69.6492, 18.9553,
                ZoneId.of("Europe/Oslo"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_ANGLE_BASED);
        assertEquals("--:--", result.Fajr);
        assertEquals("--:--", result.Isha);
    }

    @Test
    public void polarDayReturnsUnavailableTwilightAndSunrise() {
        PrayerTimings result = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 6, 15), 69.6492, 18.9553,
                ZoneId.of("Europe/Oslo"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_MIDDLE_OF_NIGHT);
        assertEquals("--:--", result.Sunrise);
        assertEquals("--:--", result.Maghrib);
    }

    @Test
    public void polarNightKeepsUnavailableSunrise() {
        PrayerTimings result = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 12, 15), 69.6492, 18.9553,
                ZoneId.of("Europe/Oslo"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_ONE_SEVENTH);
        assertEquals("--:--", result.Sunrise);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDateIsRejected() {
        PrayerCalculationEngine.calculate(null, 0, 0, ZoneId.of("UTC"), 2, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullZoneIsRejected() {
        PrayerCalculationEngine.calculate(LocalDate.of(2025, 1, 1), 0, 0, null, 2, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidLatitudeIsRejected() {
        PrayerCalculationEngine.calculate(LocalDate.of(2025, 1, 1), 91, 0, ZoneId.of("UTC"), 2, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidLongitudeIsRejected() {
        PrayerCalculationEngine.calculate(LocalDate.of(2025, 1, 1), 0, 181, ZoneId.of("UTC"), 2, 1);
    }

    @Test
    public void southernHemisphereProducesFiniteNormalTimes() {
        PrayerTimings result = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 6, 1), -34.6037, -58.3816,
                ZoneId.of("America/Argentina/Buenos_Aires"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_ONE_SEVENTH);
        assertNotEquals("--:--", result.Fajr);
        assertNotEquals("--:--", result.Isha);
        assertNotEquals("--:--", result.Dhuhr);
    }

    @Test
    public void differentTimeZonesUseTheirOwnLocalDate() {
        // Same UTC instant, different local dates around the date line.
        PrayerTimings a = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 6, 1), 0, -179, ZoneId.of("Pacific/Honolulu"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_ONE_SEVENTH);
        PrayerTimings b = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 6, 2), 0, 179, ZoneId.of("Pacific/Tarawa"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_ONE_SEVENTH);
        assertNotEquals(a.Fajr, b.Fajr);
    }
}

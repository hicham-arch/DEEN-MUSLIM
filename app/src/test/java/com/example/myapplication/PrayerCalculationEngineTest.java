package com.example.myapplication;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

public class PrayerCalculationEngineTest {
    @Test
    public void standardAndHanafiAsrDiffer() {
        PrayerTimings standard = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 6, 1), 25.2048, 55.2708,
                ZoneId.of("Asia/Dubai"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_ONE_SEVENTH);
        PrayerTimings hanafi = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 6, 1), 25.2048, 55.2708,
                ZoneId.of("Asia/Dubai"), 2, 2,
                PrayerCalculationEngine.HIGH_LATITUDE_ONE_SEVENTH);
        assertEquals("15:40", standard.Asr);
        assertEquals("16:59", hanafi.Asr);
    }

    @Test
    public void noneRuleDoesNotAdjustMissingPrayer() {
        PrayerTimings result = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 6, 15), 69.6492, 18.9553,
                ZoneId.of("Europe/Oslo"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_NONE);
        assertEquals("--:--", result.Fajr);
        assertEquals("--:--", result.Isha);
    }

    @Test
    public void oneSeventhRuleProducesAdjustedTimes() {
        PrayerTimings result = PrayerCalculationEngine.calculate(
                LocalDate.of(2025, 1, 15), 51.5074, -0.1278,
                ZoneId.of("Europe/London"), 2, 1,
                PrayerCalculationEngine.HIGH_LATITUDE_ONE_SEVENTH);
        org.junit.Assert.assertNotEquals("--:--", result.Fajr);
        org.junit.Assert.assertNotEquals("--:--", result.Isha);
    }
}

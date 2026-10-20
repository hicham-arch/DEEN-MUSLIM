package com.example.myapplication;

public class PrayerResponse {
    public int code;
    public String status;
    public Data data;

    public static class Data {
        public PrayerTimings timings;
        public DateInfo date;
        public Meta meta;
    }

    public static class DateInfo {
        public String readable;
        public Hijri hijri;
        public Gregorian gregorian;
    }

    public static class Hijri {
        public String date;
        public String day;
        public Month month;
        public String year;
    }

    public static class Month {
        public int number;
        public String en;
        public String ar;
    }

    public static class Gregorian {
        public String date;
        public String day;
        public Month month;
        public String year;
    }

    public static class Meta {
        public double latitude;
        public double longitude;
        public String timezone;
        public Method method;
    }

    public static class Method {
        public int id;
        public String name;
    }
}
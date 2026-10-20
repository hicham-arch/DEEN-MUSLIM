package com.example.myapplication;

import android.util.Log;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PrayerTimesApi {

    private static final String BASE_URL = "https://api.aladhan.com/v1";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();

    public interface Callback {
        void onSuccess(PrayerResponse response);
        void onError(String message);
    }

    /** By GPS coordinates */
    public static void getTimingsByLocation(double lat, double lng, int method, String date, Callback callback) {
        String url = BASE_URL + "/timings/" + date
                + "?latitude=" + lat
                + "&longitude=" + lng
                + "&method=" + method;

        execute(url, callback);
    }

    /** By city name */
    public static void getTimingsByCity(String city, String country, int method, String date, Callback callback) {
        String url = BASE_URL + "/timingsByCity/" + date
                + "?city=" + city.replace(" ", "%20")
                + "&country=" + country.replace(" ", "%20")
                + "&method=" + method;

        execute(url, callback);
    }

    private static void execute(String url, Callback callback) {
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code());
                    return;
                }
                String body = response.body().string();
                try {
                    PrayerResponse prayerResponse = gson.fromJson(body, PrayerResponse.class);
                    if (prayerResponse.code == 200) {
                        callback.onSuccess(prayerResponse);
                    } else {
                        callback.onError(prayerResponse.status);
                    }
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }
}
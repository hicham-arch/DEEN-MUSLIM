package com.example.myapplication;

import android.net.Uri;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class PrayerTimesApi {

    private static final String BASE_URL = "https://api.aladhan.com/v1";
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build();
    private static final Gson GSON = new Gson();

    private PrayerTimesApi() {
        // Utility class.
    }

    public interface Callback {
        void onSuccess(PrayerResponse response);
        void onError(String message);
    }

    /** Fetch timings for GPS coordinates. */
    public static void getTimingsByLocation(
            double latitude,
            double longitude,
            int method,
            String date,
            Callback callback) {

        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            callback.onError("Invalid location coordinates.");
            return;
        }

        String url = Uri.parse(BASE_URL + "/timings/" + Uri.encode(date))
                .buildUpon()
                .appendQueryParameter("latitude", String.valueOf(latitude))
                .appendQueryParameter("longitude", String.valueOf(longitude))
                .appendQueryParameter("method", String.valueOf(method))
                .build()
                .toString();

        execute(url, callback);
    }

    /** Fetch timings for a manually entered city and country. */
    public static void getTimingsByCity(
            String city,
            String country,
            int method,
            String date,
            Callback callback) {

        if (city == null || city.trim().isEmpty()
                || country == null || country.trim().isEmpty()) {
            callback.onError("Enter both a city and country.");
            return;
        }

        String url = Uri.parse(BASE_URL + "/timingsByCity/" + Uri.encode(date))
                .buildUpon()
                .appendQueryParameter("city", city.trim())
                .appendQueryParameter("country", country.trim())
                .appendQueryParameter("method", String.valueOf(method))
                .build()
                .toString();

        execute(url, callback);
    }

    private static void execute(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!call.isCanceled()) {
                    callback.onError("Could not load prayer times. Check your internet connection.");
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response closeableResponse = response) {
                    if (!closeableResponse.isSuccessful()) {
                        callback.onError("Prayer service returned HTTP "
                                + closeableResponse.code() + ".");
                        return;
                    }

                    ResponseBody body = closeableResponse.body();
                    if (body == null) {
                        callback.onError("Prayer service returned an empty response.");
                        return;
                    }

                    String json = body.string();
                    if (json.trim().isEmpty()) {
                        callback.onError("Prayer service returned an empty response.");
                        return;
                    }

                    PrayerResponse parsed;
                    try {
                        parsed = GSON.fromJson(json, PrayerResponse.class);
                    } catch (Exception parseError) {
                        callback.onError("Could not read the prayer-times response.");
                        return;
                    }

                    if (parsed == null) {
                        callback.onError("Prayer service returned invalid data.");
                    } else if (parsed.code == 200 && parsed.data != null
                            && parsed.data.timings != null) {
                        callback.onSuccess(parsed);
                    } else {
                        String message = parsed.status;
                        callback.onError(message == null || message.trim().isEmpty()
                                ? "Prayer times are unavailable for this location."
                                : message);
                    }
                } catch (IOException e) {
                    callback.onError("Could not read prayer times. Please try again.");
                }
            }
        });
    }
}

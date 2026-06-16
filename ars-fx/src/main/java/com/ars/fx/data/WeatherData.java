package com.ars.fx.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Current weather for the station location (HubConfig lat/lon) from Open-Meteo —
 * free, no API key, worldwide. Fetched off-thread, cached, refreshed every
 * 15 minutes; drives the Weather instrument drawer.
 */
public final class WeatherData {
    private static final WeatherData INSTANCE = new WeatherData();
    public static WeatherData getInstance() { return INSTANCE; }
    private WeatherData() {}

    public record Snapshot(double tempC, double feelsC, int humidity, double pressure,
                           double windKmh, int windDir, double gustKmh, String desc) {
        public String windCompass() {
            return new String[]{"N","NE","E","SE","S","SW","W","NW"}[(int) Math.round(((windDir % 360 + 360) % 360) / 45.0) % 8];
        }
    }

    private volatile Snapshot snap;
    private volatile boolean started;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public Snapshot snapshot() { return snap; }

    public synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(() -> {
            while (true) {
                try { fetch(); } catch (Throwable ignored) { }
                try { Thread.sleep(900_000L); } catch (InterruptedException e) { return; }
            }
        }, "weather-data");
        t.setDaemon(true);
        t.start();
    }

    private void fetch() throws Exception {
        String lat = HubConfig.get("station.lat", "39.7456"), lon = HubConfig.get("station.lon", "-76.96");
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,surface_pressure,"
                + "wind_speed_10m,wind_direction_10m,wind_gusts_10m&wind_speed_unit=kmh";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12))
                .header("User-Agent", "ARS-Suite-JHub").GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return;
        JsonObject c = JsonParser.parseString(resp.body()).getAsJsonObject().getAsJsonObject("current");
        snap = new Snapshot(c.get("temperature_2m").getAsDouble(), c.get("apparent_temperature").getAsDouble(),
                c.get("relative_humidity_2m").getAsInt(), c.get("surface_pressure").getAsDouble(),
                c.get("wind_speed_10m").getAsDouble(), c.get("wind_direction_10m").getAsInt(),
                c.get("wind_gusts_10m").getAsDouble(), wmo(c.get("weather_code").getAsInt()));
    }

    /** WMO weather-code → short description. */
    public static String wmo(int code) {
        if (code == 0) return "Clear";
        if (code <= 2) return "Partly cloudy";
        if (code == 3) return "Overcast";
        if (code <= 48) return "Fog";
        if (code <= 57) return "Drizzle";
        if (code <= 67) return "Rain";
        if (code <= 77) return "Snow";
        if (code <= 82) return "Showers";
        if (code <= 86) return "Snow showers";
        return "Thunderstorm";
    }
}

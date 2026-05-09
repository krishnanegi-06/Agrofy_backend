package com.agripulse.service;

import com.agripulse.model.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * ═══════════════════════════════════════════════════════════════
 * WeatherService  — MEMBER 2 MODULE (Krishna Negi)
 * ═══════════════════════════════════════════════════════════════
 *
 * Responsibilities:
 *  1. Integrate OpenWeatherMap API (free tier)
 *  2. Fetch live weather by city name:
 *       - Temperature (°C)
 *       - Humidity (%)
 *       - Rainfall probability (derived from cloudiness + weather id)
 *  3. Parse JSON response
 *  4. Return WeatherData object to IrrigationService
 *
 * API Endpoint used:
 *   GET https://api.openweathermap.org/data/2.5/weather?q={city}&appid={key}&units=metric
 *
 * Rainfall Probability Derivation:
 *   OpenWeatherMap free tier (/weather) does not return rain probability directly.
 *   We derive it from:
 *     - weather condition code (200–531 = rain/storm/drizzle → high prob)
 *     - cloudiness percentage (clouds.all)
 *   Formula: rainProb = (clouds.all × 0.5) + (isRainyCondition ? 50 : 0), capped at 100
 * ═══════════════════════════════════════════════════════════════
 */
@Service
public class WeatherService {

    private static final Logger log = Logger.getLogger(WeatherService.class.getName());

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url}")
    private String apiUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * getWeather – fetch real-time weather for a city.
     *
     * @param city  city name (e.g. "Delhi", "Dehradun")
     * @return WeatherData object — falls back to defaults if API fails
     */
    public WeatherData getWeather(String city) {
        if (city == null || city.isBlank()) {
            log.warning("No city provided — using weather defaults");
            return WeatherData.fallback("Unknown");
        }

        // Skip live call if API key is placeholder
        if ("YOUR_OPENWEATHERMAP_API_KEY".equals(apiKey)) {
            log.warning("No real API key set. Using simulated weather for: " + city);
            return simulatedWeather(city);
        }

        try {
            return fetchFromApi(city);
        } catch (Exception e) {
            log.warning("Weather API failed (" + e.getMessage() + ") — using fallback for: " + city);
            return WeatherData.fallback(city);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Live API Call
    // ─────────────────────────────────────────────────────────
    private WeatherData fetchFromApi(String city) throws Exception {
        String encodedCity = city.trim().replace(" ", "%20");
        String url = apiUrl + "?q=" + encodedCity + "&appid=" + apiKey + "&units=metric";

        log.info("Calling weather API: " + url.replace(apiKey, "***"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Weather API returned HTTP " + response.statusCode());
        }

        return parseWeatherJson(response.body(), city);
    }

    // ─────────────────────────────────────────────────────────
    // JSON Parsing
    // ─────────────────────────────────────────────────────────
    private WeatherData parseWeatherJson(String json, String city) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        WeatherData data = new WeatherData();
        data.setCity(city);
        data.setFetched(true);

        // Temperature
        double temp = root.path("main").path("temp").asDouble(25.0);
        data.setTemperatureCelsius(Math.round(temp * 10.0) / 10.0);

        // Humidity
        double humidity = root.path("main").path("humidity").asDouble(50.0);
        data.setHumidity(humidity);

        // Weather condition (for icon + rain derivation)
        JsonNode weatherArray = root.path("weather");
        int conditionCode = 800; // default = clear
        String description = "clear sky";

        if (weatherArray.isArray() && weatherArray.size() > 0) {
            JsonNode w = weatherArray.get(0);
            conditionCode = w.path("id").asInt(800);
            description   = w.path("description").asText("clear sky");
        }
        data.setDescription(capitalise(description));

        // OWM 'main' field (e.g. "Rain", "Clouds") — script.js iconMap keys on this
        String mainCondition = weatherArray.isArray() && weatherArray.size() > 0
            ? weatherArray.get(0).path("main").asText("Clouds")
            : "Clouds";
        data.setIcon(mainCondition);

        // Cloudiness
        double cloudiness = root.path("clouds").path("all").asDouble(0);

        // Rainfall probability derivation
        double rainProb = deriveRainfallProbability(conditionCode, cloudiness);
        data.setRainfallProbability(rainProb);

        log.info("Weather fetched for " + city + ": " + temp + "°C, "
                 + humidity + "% humidity, " + rainProb + "% rain prob, " + description);

        return data;
    }

    // ─────────────────────────────────────────────────────────
    // Rainfall Probability Derivation
    //   Weather codes 2xx=thunderstorm, 3xx=drizzle, 5xx=rain, 6xx=snow
    // ─────────────────────────────────────────────────────────
    private double deriveRainfallProbability(int conditionCode, double cloudiness) {
        boolean isRainyCondition = (conditionCode >= 200 && conditionCode < 700);
        double baseFromClouds    = cloudiness * 0.5;
        double rainyBonus        = isRainyCondition ? 50.0 : 0.0;
        return Math.min(100.0, baseFromClouds + rainyBonus);
    }

    // ─────────────────────────────────────────────────────────
    // Weather Condition Code → Emoji Icon
    // ─────────────────────────────────────────────────────────
    private String conditionCodeToEmoji(int code) {
        if (code >= 200 && code < 300) return "⛈️";   // Thunderstorm
        if (code >= 300 && code < 400) return "🌦️";   // Drizzle
        if (code >= 500 && code < 600) return "🌧️";   // Rain
        if (code >= 600 && code < 700) return "❄️";   // Snow
        if (code >= 700 && code < 800) return "🌫️";   // Atmosphere (fog, haze)
        if (code == 800)               return "☀️";   // Clear
        if (code == 801)               return "🌤️";   // Few clouds
        if (code == 802)               return "⛅";   // Scattered clouds
        return "☁️";                                   // Overcast / other
    }

    // ─────────────────────────────────────────────────────────
    // Simulated weather — used when API key is placeholder
    // Generates realistic data based on city name hash
    // ─────────────────────────────────────────────────────────
    private WeatherData simulatedWeather(String city) {
        WeatherData d = new WeatherData();
        d.setCity(city);
        d.setFetched(false);

        // Deterministic but varied simulation using city hash
        int hash = Math.abs(city.hashCode() % 100);
        d.setTemperatureCelsius(20.0 + (hash % 15));        // 20–35°C
        d.setHumidity(40.0 + (hash % 40));                  // 40–80%
        d.setRainfallProbability(hash % 60);                 // 0–60%
        d.setDescription("Partly cloudy (simulated — add API key for live data)");
        d.setIcon("Clouds");
        return d;
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

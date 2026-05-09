package com.agripulse.model;

/**
 * WeatherData – internal DTO passed between
 * WeatherService → IrrigationService → Controller.
 *
 * Member 2 (Krishna): Weather API Integration module.
 */
public class WeatherData {

    private double temperatureCelsius;
    private double humidity;            // 0–100 %
    private double rainfallProbability; // 0–100 %
    private String description;         // e.g. "light rain"
    private String icon;                // emoji icon
    private String city;
    private boolean fetched;            // false if API call failed

    // ── Static factory for fallback ──────────────────────
    public static WeatherData fallback(String city) {
        WeatherData d = new WeatherData();
        d.city = city;
        d.temperatureCelsius = 25.0;
        d.humidity = 50.0;
        d.rainfallProbability = 10.0;
        d.description = "Data unavailable (using defaults)";
        d.icon = "Clouds";
        d.fetched = false;
        return d;
    }

    // ── Getters & Setters ────────────────────────────────

    public double getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(double temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getRainfallProbability() { return rainfallProbability; }
    public void setRainfallProbability(double rainfallProbability) { this.rainfallProbability = rainfallProbability; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public boolean isFetched() { return fetched; }
    public void setFetched(boolean fetched) { this.fetched = fetched; }
}

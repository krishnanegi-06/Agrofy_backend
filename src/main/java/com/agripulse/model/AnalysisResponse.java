package com.agripulse.model;

/**
 * AnalysisResponse – JSON response body returned to the frontend
 * after a full soil analysis run.
 *
 * Maps exactly to what script.js expects in displayResults().
 */
public class AnalysisResponse {

    // ── Member 1: Image Processing output ───────────────
    private double avgBrightness;
    private double moisturePercent;

    // ── Member 3: Water Calculation output ──────────────
    private double waterRequiredLitres;
    private String moistureStatus;       // "Critically Dry" | "Below Optimal" | "Optimal" | "Over-saturated"
    private String idealRange;           // e.g. "50% – 70%"
    private String irrigationSchedule;   // e.g. "Every 2 days"
    private String recommendation;       // Natural language advice

    // ── Member 2: Weather API output ────────────────────
    private double temperature;
    private double humidity;
    private double rainfallProbability;
    private String weatherDescription;
    private String weatherIcon;          // emoji icon for UI

    // ── Meta ─────────────────────────────────────────────
    private String cropName;
    private String soilType;
    private double fieldAreaAcres;
    private boolean success;
    private String message;

    // ── Getters & Setters ────────────────────────────────

    public double getAvgBrightness() { return avgBrightness; }
    public void setAvgBrightness(double avgBrightness) { this.avgBrightness = avgBrightness; }

    public double getMoisturePercent() { return moisturePercent; }
    public void setMoisturePercent(double moisturePercent) { this.moisturePercent = moisturePercent; }

    public double getWaterRequiredLitres() { return waterRequiredLitres; }
    public void setWaterRequiredLitres(double waterRequiredLitres) { this.waterRequiredLitres = waterRequiredLitres; }

    public String getMoistureStatus() { return moistureStatus; }
    public void setMoistureStatus(String moistureStatus) { this.moistureStatus = moistureStatus; }

    public String getIdealRange() { return idealRange; }
    public void setIdealRange(String idealRange) { this.idealRange = idealRange; }

    public String getIrrigationSchedule() { return irrigationSchedule; }
    public void setIrrigationSchedule(String irrigationSchedule) { this.irrigationSchedule = irrigationSchedule; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getRainfallProbability() { return rainfallProbability; }
    public void setRainfallProbability(double rainfallProbability) { this.rainfallProbability = rainfallProbability; }

    public String getWeatherDescription() { return weatherDescription; }
    public void setWeatherDescription(String weatherDescription) { this.weatherDescription = weatherDescription; }

    public String getWeatherIcon() { return weatherIcon; }
    public void setWeatherIcon(String weatherIcon) { this.weatherIcon = weatherIcon; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getSoilType() { return soilType; }
    public void setSoilType(String soilType) { this.soilType = soilType; }

    public double getFieldAreaAcres() { return fieldAreaAcres; }
    public void setFieldAreaAcres(double fieldAreaAcres) { this.fieldAreaAcres = fieldAreaAcres; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

package com.agripulse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AnalysisRecord – persisted to SQLite via JPA.
 * Each row = one soil analysis run.
 * Member 4 (Kaushal): Cloud/Dashboard/Storage module.
 */
@Entity
@Table(name = "analysis_records")
public class AnalysisRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cropName;
    private String soilType;
    private double fieldAreaAcres;
    private double avgBrightness;
    private double moisturePercent;
    private double waterRequiredLitres;
    private String moistureStatus;
    private String irrigationSchedule;

    @Column(length = 512)
    private String recommendation;

    private double temperature;
    private double humidity;
    private double rainfallProbability;
    private String weatherDescription;
    private String city;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Formatted timestamp for frontend ────────────────
    @Transient
    public String getTimestamp() {
        if (createdAt == null) return "—";
        return createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    // ── Getters & Setters ────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getSoilType() { return soilType; }
    public void setSoilType(String soilType) { this.soilType = soilType; }

    public double getFieldAreaAcres() { return fieldAreaAcres; }
    public void setFieldAreaAcres(double fieldAreaAcres) { this.fieldAreaAcres = fieldAreaAcres; }

    public double getAvgBrightness() { return avgBrightness; }
    public void setAvgBrightness(double avgBrightness) { this.avgBrightness = avgBrightness; }

    public double getMoisturePercent() { return moisturePercent; }
    public void setMoisturePercent(double moisturePercent) { this.moisturePercent = moisturePercent; }

    public double getWaterRequiredLitres() { return waterRequiredLitres; }
    public void setWaterRequiredLitres(double waterRequiredLitres) { this.waterRequiredLitres = waterRequiredLitres; }

    public String getMoistureStatus() { return moistureStatus; }
    public void setMoistureStatus(String moistureStatus) { this.moistureStatus = moistureStatus; }

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

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

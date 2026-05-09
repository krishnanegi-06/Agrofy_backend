package com.agripulse.service;

import com.agripulse.model.WeatherData;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.logging.Logger;

/**
 * ═══════════════════════════════════════════════════════════════
 * IrrigationService  — MEMBER 3 MODULE (Ayush Chauhan)
 * ═══════════════════════════════════════════════════════════════
 *
 * Responsibilities:
 *  1. Accept moisture % (from ImageProcessingService) + crop ideal moisture range
 *  2. Calculate moisture deficit
 *  3. Apply water requirement formula
 *  4. Adjust water if rainfall probability is high
 *  5. Generate irrigation recommendation & schedule
 *
 * ── WATER REQUIREMENT FORMULA ───────────────────────────────
 *
 *  Variables:
 *    M_current   = current soil moisture (%)
 *    M_ideal     = ideal moisture midpoint for crop (%)
 *    deficit     = M_ideal - M_current  (negative = over-saturated)
 *    area_m2     = field area in acres × 4047 (1 acre = 4047 m²)
 *    depth_mm    = effective root zone depth (50–200 mm by soil type)
 *    volume_m3   = (deficit / 100) × area_m2 × (depth_mm / 1000)
 *    litres      = volume_m3 × 1000
 *
 *  Rainfall adjustment:
 *    If rainfallProbability > 60% → reduce water by 50%
 *    If rainfallProbability > 80% → no irrigation recommended
 *
 *  Temperature adjustment:
 *    tempFactor = 1 + ((temperature - 25) × 0.02)
 *      (hot weather increases evaporation, needs more water)
 *    Clamped to [0.8, 1.5]
 *
 *  Final: waterLitres = litres × tempFactor (after rainfall reduction)
 * ═══════════════════════════════════════════════════════════════
 */
@Service
public class IrrigationService {

    private static final Logger log = Logger.getLogger(IrrigationService.class.getName());

    // Acres to square metres
    private static final double ACRES_TO_M2 = 4047.0;

    // ── Crop Ideal Moisture Ranges (min%, max%) ──────────────
    private static final Map<String, int[]> CROP_MOISTURE = Map.ofEntries(
        Map.entry("wheat",           new int[]{50, 70}),
        Map.entry("rice",            new int[]{70, 90}),
        Map.entry("rice (paddy)",    new int[]{70, 90}),
        Map.entry("corn",            new int[]{55, 75}),
        Map.entry("corn / maize",    new int[]{55, 75}),
        Map.entry("barley",          new int[]{45, 65}),
        Map.entry("oats",            new int[]{50, 70}),
        Map.entry("sorghum",         new int[]{40, 65}),
        Map.entry("pearl millet",    new int[]{35, 60}),
        Map.entry("soybean",         new int[]{55, 75}),
        Map.entry("chickpea",        new int[]{40, 65}),
        Map.entry("lentil",          new int[]{40, 65}),
        Map.entry("potato",          new int[]{60, 80}),
        Map.entry("sweet potato",    new int[]{55, 75}),
        Map.entry("cassava",         new int[]{35, 60}),
        Map.entry("sugarcane",       new int[]{65, 85}),
        Map.entry("cotton",          new int[]{50, 70}),
        Map.entry("sunflower",       new int[]{50, 70}),
        Map.entry("canola / rapeseed",new int[]{45, 65}),
        Map.entry("tomato",          new int[]{60, 80}),
        Map.entry("onion",           new int[]{55, 75}),
        Map.entry("garlic",          new int[]{50, 70}),
        Map.entry("banana",          new int[]{65, 85}),
        Map.entry("mango",           new int[]{50, 70}),
        Map.entry("coffee",          new int[]{60, 80}),
        Map.entry("tea",             new int[]{60, 80}),
        Map.entry("turmeric",        new int[]{65, 85}),
        Map.entry("ginger",          new int[]{65, 85}),
        Map.entry("groundnut / peanut", new int[]{50, 70})
        // Default for all other crops: 50–70%
    );

    // ── Root Zone Depth by Soil Type (mm) ───────────────────
    private static final Map<String, Double> SOIL_DEPTH = Map.of(
        "sandy", 50.0,
        "loamy", 100.0,
        "clay",  150.0,
        "silt",  120.0
    );

    /**
     * calculateWaterRequirement – main method called by controller.
     *
     * @param moisturePercent     current soil moisture from image processing
     * @param cropName            crop name (matched to CROP_MOISTURE map)
     * @param fieldAreaAcres      area of field in acres
     * @param soilType            soil type string
     * @param weather             WeatherData from WeatherService
     * @return                    double[] = { waterLitres, idealMin, idealMax }
     */
    public double[] calculateWaterRequirement(double moisturePercent,
                                               String cropName,
                                               double fieldAreaAcres,
                                               String soilType,
                                               WeatherData weather) {

        // ── Step 1: Get ideal moisture range for crop ────────
        int[] range    = getCropMoistureRange(cropName);
        int idealMin   = range[0];
        int idealMax   = range[1];
        double idealMid = (idealMin + idealMax) / 2.0;

        log.info("Crop: " + cropName + " | Ideal: " + idealMin + "–" + idealMax + "%"
                 + " | Current: " + String.format("%.1f", moisturePercent) + "%");

        // ── Step 2: Calculate moisture deficit ───────────────
        double deficit = idealMid - moisturePercent;

        // ── Step 3: Convert area ──────────────────────────────
        double areaSqM  = fieldAreaAcres * ACRES_TO_M2;

        // ── Step 4: Effective root zone depth ────────────────
        double depth_mm = SOIL_DEPTH.getOrDefault(
            soilType != null ? soilType.toLowerCase() : "loamy", 100.0);

        // ── Step 5: Base water volume ─────────────────────────
        // volume (m³) = (deficit / 100) × area (m²) × depth (m)
        double waterLitres;

        if (deficit <= 0) {
            waterLitres = 0;  // Soil already at or above ideal — no irrigation
        } else {
            double volumeM3 = (deficit / 100.0) * areaSqM * (depth_mm / 1000.0);
            waterLitres = volumeM3 * 1000.0;
        }

        // ── Step 6: Temperature adjustment ───────────────────
        if (waterLitres > 0 && weather != null) {
            double tempFactor = 1.0 + ((weather.getTemperatureCelsius() - 25.0) * 0.02);
            tempFactor = Math.max(0.8, Math.min(1.5, tempFactor));  // Clamp
            waterLitres *= tempFactor;
            log.info("Temperature factor: " + String.format("%.2f", tempFactor));
        }

        // ── Step 7: Rainfall adjustment ──────────────────────
        if (weather != null) {
            double rainProb = weather.getRainfallProbability();
            if (rainProb > 80) {
                waterLitres = 0;   // Skip irrigation — rain expected
                log.info("Rain prob " + rainProb + "% > 80% — skipping irrigation");
            } else if (rainProb > 60) {
                waterLitres *= 0.5;   // Reduce by half
                log.info("Rain prob " + rainProb + "% > 60% — halving water requirement");
            }
        }

        waterLitres = Math.max(0, waterLitres);
        log.info("Final water required: " + String.format("%.0f", waterLitres) + " litres");

        return new double[]{ waterLitres, idealMin, idealMax };
    }

    /**
     * getMoistureStatus – classifies current moisture vs crop ideal.
     */
    public String getMoistureStatus(double moisture, int idealMin, int idealMax) {
        if      (moisture < idealMin - 15) return "Critically Dry";
        else if (moisture < idealMin)      return "Below Optimal";
        else if (moisture <= idealMax)     return "Optimal";
        else                               return "Over-saturated";
    }

    /**
     * getIrrigationSchedule – returns a human-readable frequency string.
     */
    public String getIrrigationSchedule(double moisture, int idealMin,
                                         String soilType, WeatherData weather) {
        double rainProb = (weather != null) ? weather.getRainfallProbability() : 0;

        if (rainProb > 80)            return "Skip — rain expected";
        if (moisture >= idealMin)     return "No irrigation needed";

        double deficit = idealMin - moisture;
        if      (deficit > 30) return "Irrigate immediately";
        else if (deficit > 15) return "Every day for 3 days";
        else if (deficit > 5)  return "Every 2 days";
        else                   return "Light irrigation in 3 days";
    }

    /**
     * buildRecommendation – generates the natural language advice shown in the UI.
     */
    public String buildRecommendation(double moisture, int idealMin, int idealMax,
                                       double waterLitres, String cropName,
                                       WeatherData weather) {
        String status = getMoistureStatus(moisture, idealMin, idealMax);
        StringBuilder sb = new StringBuilder();

        switch (status) {
            case "Critically Dry" -> sb.append("⚠️ Soil is critically dry. Irrigate immediately. ");
            case "Below Optimal"  -> sb.append("💧 Moisture is below the ideal range. Schedule irrigation soon. ");
            case "Optimal"        -> sb.append("✅ Soil moisture is in the optimal range. No irrigation required. ");
            case "Over-saturated" -> sb.append("🌊 Soil is over-saturated. Avoid irrigation and improve drainage. ");
        }

        if (waterLitres > 0) {
            sb.append(String.format("Apply approximately %.0f litres for your %.1f-acre field. ",
                waterLitres, 0.0)); // fieldArea not passed here — keep it simple
        }

        if (weather != null && weather.getRainfallProbability() > 60) {
            sb.append(String.format("Note: %.0f%% chance of rainfall — reduce or skip irrigation. ",
                weather.getRainfallProbability()));
        }

        if (weather != null && weather.getTemperatureCelsius() > 35) {
            sb.append("High temperature detected — irrigate in early morning or evening to reduce evaporation.");
        }

        return sb.toString().trim();
    }

    // ─────────────────────────────────────────────────────────
    // Helper: get ideal moisture range for crop by name
    // ─────────────────────────────────────────────────────────
    private int[] getCropMoistureRange(String cropName) {
        if (cropName == null) return new int[]{50, 70};
        String key = cropName.toLowerCase().trim();

        // Direct match
        if (CROP_MOISTURE.containsKey(key)) return CROP_MOISTURE.get(key);

        // Partial match (e.g. "Rice (Paddy)" matches "rice")
        for (Map.Entry<String, int[]> entry : CROP_MOISTURE.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }

        // Default
        return new int[]{50, 70};
    }

    public int[] getCropRangePublic(String cropName) {
        return getCropMoistureRange(cropName);
    }
}

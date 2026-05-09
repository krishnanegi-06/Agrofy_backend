package com.agripulse.controller;

import com.agripulse.model.AnalysisRecord;
import com.agripulse.model.AnalysisResponse;
import com.agripulse.model.WeatherData;
import com.agripulse.service.AnalysisRepository;
import com.agripulse.service.ImageProcessingService;
import com.agripulse.service.IrrigationService;
import com.agripulse.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * ═══════════════════════════════════════════════════════════════
 * AgripulseController – Main REST API Controller
 * ═══════════════════════════════════════════════════════════════
 *
 * Exposes all endpoints called by script.js frontend.
 * Orchestrates all four member modules in sequence:
 *
 *   POST /api/analyze
 *     → Member 1: ImageProcessingService  (brightness → moisture %)
 *     → Member 2: WeatherService          (city → live weather)
 *     → Member 3: IrrigationService       (moisture + weather → water litres)
 *     → Member 4: AnalysisRepository      (save record to SQLite)
 *
 *   GET  /api/health         → connection check
 *   GET  /api/history        → all past records
 *   GET  /api/dashboard      → summary stats
 *   GET  /api/weather/{city} → standalone weather fetch
 * ═══════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AgripulseController {

    private static final Logger log = Logger.getLogger(AgripulseController.class.getName());

    @Autowired private ImageProcessingService imageProcessingService;
    @Autowired private WeatherService          weatherService;
    @Autowired private IrrigationService       irrigationService;
    @Autowired private AnalysisRepository      analysisRepository;

    // ═══════════════════════════════════════════════════════
    // 1. HEALTH CHECK  — GET /api/health
    //    Called by script.js on page load to check if backend is up.
    //    Green dot = connected, red dot = not connected.
    // ═══════════════════════════════════════════════════════
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        long totalRecords = analysisRepository.count();
        return ResponseEntity.ok(Map.of(
            "status",  "UP",
            "service", "AgroPulse Backend",
            "version", "1.0.0",
            "records", totalRecords
        ));
    }

    // ═══════════════════════════════════════════════════════
    // 2. MAIN ANALYSIS  — POST /api/analyze
    //    Accepts multipart form with image + config fields.
    //    Runs all 4 modules and returns AnalysisResponse JSON.
    //
    //    Form fields (from script.js FormData):
    //      image       — soil image file (Member 1)
    //      cropName    — e.g. "Wheat"
    //      fieldArea   — field area in acres
    //      city        — city name for weather (Member 2)
    //      soilType    — "loamy" | "sandy" | "clay" | "silt"
    //      idealMin    — crop ideal moisture min %
    //      idealMax    — crop ideal moisture max %
    // ═══════════════════════════════════════════════════════
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(
            @RequestParam("image")    MultipartFile image,
            @RequestParam("cropName") String cropName,
            @RequestParam("area")     double fieldArea,   // FormData sends "area"
            @RequestParam("city")     String city,
            @RequestParam("soilType") String soilType,
            @RequestParam(value = "idealMin", defaultValue = "0") int idealMin,
            @RequestParam(value = "idealMax", defaultValue = "0") int idealMax) {

        log.info("═══ Analysis Request ═══");
        log.info("Crop: " + cropName + " | Area: " + fieldArea + " acres"
                 + " | City: " + city + " | Soil: " + soilType);

        AnalysisResponse response = new AnalysisResponse();

        try {

            // ── MEMBER 1: Image Processing ─────────────────────
            log.info("[Member 1] Running image processing...");
            double[] imageResult = imageProcessingService.processImage(image, soilType);
            double avgBrightness  = imageResult[0];
            double moisturePercent = imageResult[1];

            response.setAvgBrightness(avgBrightness);
            response.setMoisturePercent(moisturePercent);

            // ── MEMBER 2: Weather API ──────────────────────────
            log.info("[Member 2] Fetching weather for: " + city);
            WeatherData weather = weatherService.getWeather(city);

            response.setTemperature(weather.getTemperatureCelsius());
            response.setHumidity(weather.getHumidity());
            response.setRainfallProbability(weather.getRainfallProbability());
            response.setWeatherDescription(weather.getDescription());
            response.setWeatherIcon(weather.getIcon());

            // ── MEMBER 3: Water Calculation ────────────────────
            log.info("[Member 3] Calculating water requirement...");
            double[] waterResult = irrigationService.calculateWaterRequirement(
                moisturePercent, cropName, fieldArea, soilType, weather);

            double waterLitres      = waterResult[0];
            int    cropIdealMin     = (idealMin > 0) ? idealMin : (int) waterResult[1];
            int    cropIdealMax     = (idealMax > 0) ? idealMax : (int) waterResult[2];

            String moistureStatus   = irrigationService.getMoistureStatus(moisturePercent, cropIdealMin, cropIdealMax);
            String irrigationSched  = irrigationService.getIrrigationSchedule(moisturePercent, cropIdealMin, soilType, weather);
            String recommendation   = irrigationService.buildRecommendation(
                                        moisturePercent, cropIdealMin, cropIdealMax,
                                        waterLitres, cropName, weather);

            response.setWaterRequiredLitres(waterLitres);
            response.setMoistureStatus(moistureStatus);
            response.setIdealRange(cropIdealMin + "% – " + cropIdealMax + "%");
            response.setIrrigationSchedule(irrigationSched);
            response.setRecommendation(recommendation);
            response.setCropName(cropName);
            response.setSoilType(soilType);
            response.setFieldAreaAcres(fieldArea);

            // ── MEMBER 4: Save to SQLite database ─────────────
            log.info("[Member 4] Saving record to database...");
            saveRecord(response, weather, city);

            response.setSuccess(true);
            response.setMessage("Analysis complete");
            log.info("═══ Analysis Complete ═══ Moisture: "
                     + String.format("%.1f", moisturePercent) + "% | Water: "
                     + String.format("%.0f", waterLitres) + "L");

        } catch (Exception e) {
            log.severe("Analysis failed: " + e.getMessage());
            response.setSuccess(false);
            response.setMessage("Analysis failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════
    // 3. HISTORY  — GET /api/history
    //    Returns all past analysis records from SQLite.
    //    Used to populate the History table in the frontend.
    // ═══════════════════════════════════════════════════════
    @GetMapping("/history")
    public ResponseEntity<List<AnalysisRecord>> getHistory() {
        List<AnalysisRecord> records = analysisRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(records);
    }

    // ═══════════════════════════════════════════════════════
    // 4. DASHBOARD  — GET /api/dashboard
    //    Returns aggregate stats for the IoT Dashboard section.
    //    Called by "Send to Cloud" button in script.js.
    // ═══════════════════════════════════════════════════════
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Double avgMoisture    = analysisRepository.findAverageMoisture();
        Double totalWater     = analysisRepository.findTotalWaterRequired();
        Long   totalAnalyzed  = analysisRepository.count();
        Long   distinctCrops  = analysisRepository.countDistinctCrops();
        List<AnalysisRecord> recent = analysisRepository.findTop10ByOrderByCreatedAtDesc();

        // Latest weather from last record
        String latestTemp     = "—";
        String latestHumidity = "—";
        String latestRainfall = "—";

        if (!recent.isEmpty()) {
            AnalysisRecord last = recent.get(0);
            latestTemp     = String.format("%.1f", last.getTemperature());
            latestHumidity = String.format("%.0f", last.getHumidity());
            latestRainfall = String.format("%.0f", last.getRainfallProbability());
        }

        return ResponseEntity.ok(Map.of(
            "totalAnalyzed",    totalAnalyzed,
            "avgMoisture",      avgMoisture  != null ? String.format("%.1f%%", avgMoisture)  : "—",
            "totalWaterLitres", totalWater   != null ? String.format("%.0f",   totalWater)   : "0",
            "distinctCrops",    distinctCrops,
            "latestTemp",       latestTemp + "°C",
            "latestHumidity",   latestHumidity + "%",
            "latestRainfall",   latestRainfall + "%",
            "recentRecords",    recent
        ));
    }

    // ═══════════════════════════════════════════════════════
    // 5. STANDALONE WEATHER  — GET /api/weather/{city}
    //    Allows frontend to fetch weather independently.
    // ═══════════════════════════════════════════════════════
    @GetMapping("/weather/{city}")
    public ResponseEntity<Map<String, Object>> getWeather(@PathVariable String city) {
        WeatherData w = weatherService.getWeather(city);
        return ResponseEntity.ok(Map.of(
            "city",               w.getCity(),
            "temperature",        w.getTemperatureCelsius(),
            "humidity",           w.getHumidity(),
            "rainfallProbability",w.getRainfallProbability(),
            "description",        w.getDescription(),
            "icon",               w.getIcon(),
            "fetched",            w.isFetched()
        ));
    }

    // ─────────────────────────────────────────────────────────
    // Private helper: save analysis result to SQLite
    // ─────────────────────────────────────────────────────────
    private void saveRecord(AnalysisResponse resp, WeatherData weather, String city) {
        AnalysisRecord record = new AnalysisRecord();
        record.setCropName(resp.getCropName());
        record.setSoilType(resp.getSoilType());
        record.setFieldAreaAcres(resp.getFieldAreaAcres());
        record.setAvgBrightness(resp.getAvgBrightness());
        record.setMoisturePercent(resp.getMoisturePercent());
        record.setWaterRequiredLitres(resp.getWaterRequiredLitres());
        record.setMoistureStatus(resp.getMoistureStatus());
        record.setIrrigationSchedule(resp.getIrrigationSchedule());
        record.setRecommendation(resp.getRecommendation());
        record.setTemperature(weather.getTemperatureCelsius());
        record.setHumidity(weather.getHumidity());
        record.setRainfallProbability(weather.getRainfallProbability());
        record.setWeatherDescription(weather.getDescription());
        record.setCity(city);
        analysisRepository.save(record);
        log.info("[Member 4] Record saved to SQLite: id=" + record.getId());
    }
}

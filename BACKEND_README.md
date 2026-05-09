# 🌱 AgroPulse – Java Spring Boot Backend

## Team Module Map
| Member | Module | Java File |
|--------|--------|-----------|
| Niharika (Lead) | Image Processing | `ImageProcessingService.java` |
| Krishna Negi | Weather API | `WeatherService.java` |
| Ayush Chauhan | Water Calculation | `IrrigationService.java` |
| Kaushal Singh | Cloud / Dashboard / DB | `AnalysisRepository.java` |

---

## Quick Start

### Prerequisites
- Java 21+ (`java --version`)
- Maven 3.8+ (`mvn --version`)

### Step 1 – Get a FREE Weather API Key
1. Go to https://openweathermap.org/api and sign up (free)
2. Copy your API key
3. Open `src/main/resources/application.properties`
4. Replace `YOUR_OPENWEATHERMAP_API_KEY` with your real key

> **Without a real key:** The backend still works! It uses simulated weather data based on city name.

### Step 2 – Build & Run
```bash
cd agripulse-backend
mvn spring-boot:run
```

Server starts at: **http://localhost:8080**

### Step 3 – Connect Frontend
Open `index.html` in a browser (or serve it via Live Server in VS Code).
The frontend already points to `http://localhost:8080/api`.

---

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| `GET`  | `/api/health` | Connection check (green/red dot in UI) |
| `POST` | `/api/analyze` | Run full soil analysis (main endpoint) |
| `GET`  | `/api/history` | All past analysis records |
| `GET`  | `/api/dashboard` | Aggregate stats for IoT dashboard |
| `GET`  | `/api/weather/{city}` | Standalone weather fetch |

### POST /api/analyze – Form Fields
```
image      = soil image file (JPG/PNG, max 10MB)
cropName   = e.g. "Wheat"
fieldArea  = field area in acres, e.g. 2.5
city       = city name for live weather, e.g. "Dehradun"
soilType   = loamy | sandy | clay | silt
idealMin   = (optional) crop ideal moisture min %
idealMax   = (optional) crop ideal moisture max %
```

### Response JSON
```json
{
  "avgBrightness": 112.4,
  "moisturePercent": 55.9,
  "waterRequiredLitres": 23450,
  "moistureStatus": "Below Optimal",
  "idealRange": "50% – 70%",
  "irrigationSchedule": "Every 2 days",
  "recommendation": "💧 Moisture is below the ideal range...",
  "temperature": 28.5,
  "humidity": 62.0,
  "rainfallProbability": 15.0,
  "weatherDescription": "Partly cloudy",
  "weatherIcon": "⛅",
  "success": true
}
```

---

## How the Moisture Formula Works (Member 1)

```
Dark soil (low brightness)  → HIGH moisture
Light soil (high brightness) → LOW moisture

Base moisture = (1 - avgBrightness / 255) × 100
Adjusted      = Base × soilTypeFactor

Soil factors:
  Sandy = ×0.80 (drains fast)
  Loamy = ×1.00 (baseline)
  Clay  = ×1.15 (retains water)
  Silt  = ×1.05
```

## How the Water Formula Works (Member 3)

```
deficit    = idealMoistureMiddle - currentMoisture
volume_m³  = (deficit/100) × areaM² × rootDepthM
litres     = volume_m³ × 1000

Adjustments:
  Hot weather (>25°C)    → +2% per degree (more evaporation)
  Rain prob >60%         → reduce by 50%
  Rain prob >80%         → skip irrigation
```

---

## Database

SQLite file created automatically at: `agripulse.db`  
No setup needed. Spring JPA handles table creation on first run.

---

## Project Structure

```
agripulse-backend/
├── pom.xml
└── src/main/
    ├── java/com/agripulse/
    │   ├── AgripulseApplication.java       ← Main entry point
    │   ├── config/
    │   │   └── CorsConfig.java             ← CORS for frontend
    │   ├── controller/
    │   │   └── AgripulseController.java    ← All REST endpoints
    │   ├── model/
    │   │   ├── AnalysisRecord.java         ← Database entity
    │   │   ├── AnalysisResponse.java       ← API response shape
    │   │   └── WeatherData.java            ← Weather DTO
    │   └── service/
    │       ├── ImageProcessingService.java ← Member 1
    │       ├── WeatherService.java         ← Member 2
    │       ├── IrrigationService.java      ← Member 3
    │       └── AnalysisRepository.java     ← Member 4
    └── resources/
        └── application.properties
```

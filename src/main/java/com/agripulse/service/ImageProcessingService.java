package com.agripulse.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;

/*
 ═══════════════════════════════════════════════════════════════
  ImageProcessingService  
  ═══════════════════════════════════════════════════════════════
 
  Responsibilities:
   1. Accept uploaded soil image (JPG / PNG)
   2. Convert image to grayscale
   3. Extract pixel intensity values
   4. Calculate average brightness (0–255)
   5. Map brightness → soil moisture percentage (0–100%)
 
  Formula Explanation:
   ─────────────────────────────────────────────────────────────
   Dark soil (low brightness) = HIGH moisture content
   Light/dry soil (high brightness) = LOW moisture content
 
   
   Raw moisture ratio  = 1 - (avgBrightness / 255)
   Moisture %          = rawRatio × 100
 
   Soil type correction factor:
     Sandy  → ×0.80  (drains fast, appears lighter when dry)
     Loamy  → ×1.00  (baseline – ideal soil)
     Clay   → ×1.15  (retains water, darker when moist)
     Silt   → ×1.05  (moderate retention)
  ─────────────────────────────────────────────────────────────
 */
@Service
public class ImageProcessingService {

    private static final Logger log = Logger.getLogger(ImageProcessingService.class.getName());

    // Soil type correction multipliers
    private static final java.util.Map<String, Double> SOIL_FACTORS = java.util.Map.of(
        "sandy", 0.80,
        "loamy", 1.00,
        "clay",  1.15,
        "silt",  1.05
    );

    /*
      processImage – main entry point called by the controller.
     
      @param imageFile  multipart image uploaded by user
      @param soilType   "loamy" | "sandy" | "clay" | "silt"
      @return           double[2] = { avgBrightness, moisturePercent }
      @throws IOException if image cannot be read
     */
    public double[] processImage(MultipartFile imageFile, String soilType) throws IOException {
        log.info("Processing image: " + imageFile.getOriginalFilename()
                 + " | Size: " + imageFile.getSize() + " bytes | Soil: " + soilType);

        // ── Step 1: Read image ────────────────────────────────
        BufferedImage originalImage = ImageIO.read(imageFile.getInputStream());

        if (originalImage == null) {
            throw new IOException("Could not read image file. Ensure it is a valid JPG or PNG.");
        }

        int width  = originalImage.getWidth();
        int height = originalImage.getHeight();
        log.info("Image dimensions: " + width + "×" + height + " px");

        // ── Step 2: Convert to grayscale ──────────────────────
        BufferedImage grayImage = toGrayscale(originalImage, width, height);

        // ── Step 3: Extract pixel intensity values ────────────
        // ── Step 4: Calculate average brightness ─────────────
        double avgBrightness = calculateAverageBrightness(grayImage, width, height);
        log.info("Average brightness: " + String.format("%.2f", avgBrightness));

        // ── Step 5: Map brightness → moisture % ──────────────
        double moisturePercent = brightnessToMoisture(avgBrightness, soilType);
        log.info("Moisture percent: " + String.format("%.2f", moisturePercent) + "%");

        return new double[]{ avgBrightness, moisturePercent };
    }

    // ─────────────────────────────────────────────────────────
    // STEP 2: Convert to Grayscale
    //   Using luminosity formula: gray = 0.2126R + 0.7152G + 0.0722B
    //   (matches human eye perception — more accurate than average)
    // ─────────────────────────────────────────────────────────
    private BufferedImage toGrayscale(BufferedImage original, int width, int height) {
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(original.getRGB(x, y), true);
                int grayValue = (int)(
                    0.2126 * color.getRed() +
                    0.7152 * color.getGreen() +
                    0.0722 * color.getBlue()
                );
                // Pack grayscale value back into RGB (R=G=B=gray)
                int grayRGB = (grayValue << 16) | (grayValue << 8) | grayValue;
                gray.setRGB(x, y, grayRGB);
            }
        }
        return gray;
    }

    // ─────────────────────────────────────────────────────────
    // STEP 3 + 4: Extract pixel intensities & average them
    //   Uses streaming over all pixels for performance.
    //   Samples every 2nd pixel on large images to stay fast.
    // ─────────────────────────────────────────────────────────
    private double calculateAverageBrightness(BufferedImage gray, int width, int height) {
        long totalBrightness = 0;
        long pixelCount = 0;

        // Sample step: 1 for small images, 2 for large (>1 MP)
        int step = (width * height > 1_000_000) ? 2 : 1;

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                // Grayscale image: red channel = gray intensity
                int pixelValue = new Color(gray.getRGB(x, y)).getRed();
                totalBrightness += pixelValue;
                pixelCount++;
            }
        }

        return (pixelCount > 0) ? (double) totalBrightness / pixelCount : 128.0;
    }

    // ─────────────────────────────────────────────────────────
    // STEP 5: Brightness → Moisture Formula
    //
    //   Base moisture = (1 - brightness/255) × 100
    //   Adjusted      = Base × soilFactor
    //   Clamped       = [0, 100]
    // ─────────────────────────────────────────────────────────
    private double brightnessToMoisture(double avgBrightness, String soilType) {
        double baseMoisture = (1.0 - (avgBrightness / 255.0)) * 100.0;

        double factor = SOIL_FACTORS.getOrDefault(
            soilType != null ? soilType.toLowerCase() : "loamy",
            1.0
        );

        double adjusted = baseMoisture * factor;

        // Clamp to valid range
        return Math.max(0.0, Math.min(100.0, adjusted));
    }
}

package com.agripulse.service;

import com.agripulse.model.AnalysisRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 * AnalysisRepository  — MEMBER 4 MODULE (Kaushal Singh) — Part A
 * ═══════════════════════════════════════════════════════════════
 *
 * Spring Data JPA repository for persisting AnalysisRecord
 * entities to SQLite database (agripulse.db).
 *
 * All CRUD operations are automatically provided by JPA.
 * Custom queries are added below for dashboard reporting.
 */
@Repository
public interface AnalysisRepository extends JpaRepository<AnalysisRecord, Long> {

    /** All records, newest first — used for history table */
    List<AnalysisRecord> findAllByOrderByCreatedAtDesc();

    /** Last 10 records for dashboard */
    List<AnalysisRecord> findTop10ByOrderByCreatedAtDesc();

    /** Average moisture across all readings */
    @Query("SELECT AVG(a.moisturePercent) FROM AnalysisRecord a")
    Double findAverageMoisture();

    /** Total water recommended (sum of all runs) */
    @Query("SELECT SUM(a.waterRequiredLitres) FROM AnalysisRecord a")
    Double findTotalWaterRequired();

    /** Count distinct crops analyzed */
    @Query("SELECT COUNT(DISTINCT a.cropName) FROM AnalysisRecord a")
    Long countDistinctCrops();
}

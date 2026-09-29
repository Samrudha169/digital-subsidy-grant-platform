package com.dsgp.disbursement.service;

import com.dsgp.disbursement.dto.DisbursementAnalyticsResponse;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementStage;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import com.dsgp.disbursement.repository.DisbursementStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates read-only disbursement analytics using existing
 * repositories and entity relationships.
 *
 * <p>No database schema changes, no new columns, no new tables.
 * All aggregations are performed in-memory from existing data.
 */
@Service
@RequiredArgsConstructor
public class DisbursementAnalyticsService {

    private final DisbursementPlanRepository disbursementPlanRepository;
    private final DisbursementStageRepository disbursementStageRepository;

    /**
     * Returns a full analytics snapshot covering:
     * <ul>
     *   <li>Total sanctioned, planned, released, and remaining amounts</li>
     *   <li>Released amount broken down by scheme</li>
     *   <li>Released amount broken down by beneficiary state</li>
     *   <li>Released amount broken down by beneficiary district</li>
     *   <li>Released amount broken down by beneficiary village</li>
     * </ul>
     *
     * @return {@link DisbursementAnalyticsResponse} with all metrics
     */
    @Transactional(readOnly = true)
    public DisbursementAnalyticsResponse getAnalytics() {

        List<DisbursementPlan> plans = disbursementPlanRepository.findAll();
        List<DisbursementStage> stages = disbursementStageRepository.findAll();

        BigDecimal totalSanctioned = BigDecimal.ZERO;
        BigDecimal totalReleased   = BigDecimal.ZERO;
        BigDecimal totalRemaining  = BigDecimal.ZERO;

        Map<String, BigDecimal> releasedByScheme   = new HashMap<>();
        Map<String, BigDecimal> releasedByState    = new HashMap<>();
        Map<String, BigDecimal> releasedByDistrict = new HashMap<>();
        Map<String, BigDecimal> releasedByVillage  = new HashMap<>();

        for (DisbursementPlan plan : plans) {

            // ── Plan-level totals ──────────────────────────────────────────

            totalSanctioned = totalSanctioned.add(
                    nullSafe(plan.getTotalAmount()));

            totalReleased = totalReleased.add(
                    nullSafe(plan.getReleasedAmount()));

            totalRemaining = totalRemaining.add(
                    nullSafe(plan.getRemainingAmount()));

            BigDecimal released = nullSafe(plan.getReleasedAmount());

            if (released.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // ── Released by scheme ─────────────────────────────────────────

            String schemeName = resolveSchemeName(plan);

            releasedByScheme.merge(schemeName, released, BigDecimal::add);

            // ── Released by state ──────────────────────────────────────────

            String state = resolveState(plan);

            releasedByState.merge(state, released, BigDecimal::add);

            // ── Released by district ───────────────────────────────────────

            String district = resolveDistrict(plan);

            releasedByDistrict.merge(district, released, BigDecimal::add);

            // ── Released by village ────────────────────────────────────────

            String village = resolveVillage(plan);

            releasedByVillage.merge(village, released, BigDecimal::add);
        }

        // ── Total planned = sum of all stage amounts ───────────────────────

        BigDecimal totalPlanned = stages.stream()
                .map(DisbursementStage::getAmount)
                .map(this::nullSafe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DisbursementAnalyticsResponse.builder()
                .totalSanctioned(totalSanctioned)
                .totalPlanned(totalPlanned)
                .totalReleased(totalReleased)
                .totalRemaining(totalRemaining)
                .releasedByScheme(releasedByScheme)
                .releasedByState(releasedByState)
                .releasedByDistrict(releasedByDistrict)
                .releasedByVillage(releasedByVillage)
                .build();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Navigates plan → application → scheme to get the scheme name.
     * Falls back to "Unknown Scheme" if the relationship is not loaded.
     */
    private String resolveSchemeName(DisbursementPlan plan) {
        try {
            String name = plan.getApplication()
                    .getScheme()
                    .getSchemeName();
            return (name != null && !name.isBlank()) ? name : "Unknown Scheme";
        } catch (Exception e) {
            return "Unknown Scheme";
        }
    }

    /**
     * Navigates plan → application → beneficiary to get the state.
     * Falls back to "Unknown State" if the field is not populated.
     */
    private String resolveState(DisbursementPlan plan) {
        try {
            String state = plan.getApplication()
                    .getBeneficiary()
                    .getState();
            return (state != null && !state.isBlank()) ? state : "Unknown State";
        } catch (Exception e) {
            return "Unknown State";
        }
    }

    /**
     * Navigates plan → application → beneficiary to get the district.
     * Falls back to "Unknown District" if the field is null or blank.
     */
    private String resolveDistrict(DisbursementPlan plan) {
        try {
            String district = plan.getApplication()
                    .getBeneficiary()
                    .getDistrict();
            return (district != null && !district.isBlank()) ? district : "Unknown District";
        } catch (Exception e) {
            return "Unknown District";
        }
    }

    /**
     * Navigates plan → application → beneficiary to get the village.
     * Falls back to "Unknown Village" if the field is null or blank.
     */
    private String resolveVillage(DisbursementPlan plan) {
        try {
            String village = plan.getApplication()
                    .getBeneficiary()
                    .getVillage();
            return (village != null && !village.isBlank()) ? village : "Unknown Village";
        } catch (Exception e) {
            return "Unknown Village";
        }
    }

    /** Treats null BigDecimal as zero. */
    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}

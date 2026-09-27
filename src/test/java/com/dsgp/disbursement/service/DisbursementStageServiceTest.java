package com.dsgp.disbursement.service;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.disbursement.entity.ComplianceStatus;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementStage;
import com.dsgp.disbursement.entity.DisbursementStageStatus;
import com.dsgp.disbursement.entity.DisbursementStatus;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import com.dsgp.disbursement.repository.DisbursementStageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisbursementStageService")
class DisbursementStageServiceTest {

    @Mock
    private DisbursementPlanRepository disbursementPlanRepository;

    @Mock
    private DisbursementStageRepository disbursementStageRepository;

    @InjectMocks
    private DisbursementStageService disbursementStageService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Beneficiary beneficiary() {
        Beneficiary b = new Beneficiary();
        b.setId(1);
        b.setFullName("Ravi Kumar");
        b.setState("Maharashtra");
        return b;
    }

    private Scheme scheme() {
        Scheme s = new Scheme();
        s.setId(1L);
        s.setSchemeName("PM-KISAN");
        return s;
    }

    private SchemeApplication approvedApp(BigDecimal sanctioned) {
        SchemeApplication app = SchemeApplication.builder()
                .beneficiary(beneficiary())
                .scheme(scheme())
                .applicationStatus("APPROVED")
                .sanctionedAmount(sanctioned)
                .build();
        app.setId(10L);
        return app;
    }

    private DisbursementPlan stagedPlan(BigDecimal total) {
        SchemeApplication app = approvedApp(total);
        DisbursementPlan plan = DisbursementPlan.builder()
                .application(app)
                .totalAmount(total)
                .releasedAmount(BigDecimal.ZERO)
                .remainingAmount(total)
                .disbursementType(DisbursementType.STAGED)
                .status(DisbursementStatus.PENDING)
                .build();
        plan.setId(1L);
        return plan;
    }

    private DisbursementStage pendingStage(DisbursementPlan plan, BigDecimal amount, int stageNum) {
        DisbursementStage stage = DisbursementStage.builder()
                .disbursementPlan(plan)
                .stageNumber(stageNum)
                .amount(amount)
                .milestone("Milestone " + stageNum)
                .dueDate(LocalDate.now().plusDays(30))
                .status(DisbursementStageStatus.PENDING)
                .complianceStatus(ComplianceStatus.PENDING)
                .build();
        stage.setId((long) stageNum);
        return stage;
    }

    private DisbursementStage verifiedStage(DisbursementPlan plan, BigDecimal amount) {
        DisbursementStage stage = DisbursementStage.builder()
                .disbursementPlan(plan)
                .stageNumber(1)
                .amount(amount)
                .milestone("Verified Milestone")
                .dueDate(LocalDate.now().plusDays(30))
                .status(DisbursementStageStatus.VERIFIED)
                .complianceStatus(ComplianceStatus.PENDING)
                .build();
        stage.setId(1L);
        return stage;
    }

    // ── CreateStage ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createStage()")
    class CreateStage {

        @Test
        @DisplayName("creates a valid stage and returns it")
        void validInputs_returnsCreatedStage() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.findById(1L)).willReturn(Optional.of(plan));
            given(disbursementStageRepository.findByDisbursementPlanIdAndStageNumber(1L, 1))
                    .willReturn(Optional.empty());
            given(disbursementStageRepository.findByDisbursementPlanIdOrderByStageNumberAsc(1L))
                    .willReturn(List.of());
            given(disbursementStageRepository.save(any(DisbursementStage.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DisbursementStage result = disbursementStageService.createStage(
                    1L, 1, new BigDecimal("2000.00"), "First Instalment", LocalDate.now().plusDays(30));

            assertThat(result).isNotNull();
            assertThat(result.getStageNumber()).isEqualTo(1);
            assertThat(result.getAmount()).isEqualByComparingTo("2000.00");
            assertThat(result.getMilestone()).isEqualTo("First Instalment");
        }

        @Test
        @DisplayName("stores the provided due date on the stage")
        void validInputs_storesDueDate() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            LocalDate dueDate = LocalDate.now().plusDays(60);
            given(disbursementPlanRepository.findById(1L)).willReturn(Optional.of(plan));
            given(disbursementStageRepository.findByDisbursementPlanIdAndStageNumber(1L, 1))
                    .willReturn(Optional.empty());
            given(disbursementStageRepository.findByDisbursementPlanIdOrderByStageNumberAsc(1L))
                    .willReturn(List.of());
            given(disbursementStageRepository.save(any(DisbursementStage.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DisbursementStage result = disbursementStageService.createStage(
                    1L, 1, new BigDecimal("2000.00"), "Milestone", dueDate);

            assertThat(result.getDueDate()).isEqualTo(dueDate);
        }

        @Test
        @DisplayName("initial stage status is PENDING")
        void validInputs_initialStatusIsPending() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.findById(1L)).willReturn(Optional.of(plan));
            given(disbursementStageRepository.findByDisbursementPlanIdAndStageNumber(1L, 1))
                    .willReturn(Optional.empty());
            given(disbursementStageRepository.findByDisbursementPlanIdOrderByStageNumberAsc(1L))
                    .willReturn(List.of());
            given(disbursementStageRepository.save(any(DisbursementStage.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DisbursementStage result = disbursementStageService.createStage(
                    1L, 1, new BigDecimal("2000.00"), "Milestone", LocalDate.now().plusDays(30));

            assertThat(result.getStatus()).isEqualTo(DisbursementStageStatus.PENDING);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when plan is not found")
        void planNotFound_throwsIllegalArgumentException() {
            given(disbursementPlanRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> disbursementStageService.createStage(
                    99L, 1, new BigDecimal("2000.00"), "Milestone", LocalDate.now().plusDays(30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("throws IllegalStateException when plan type is SINGLE")
        void singleTypePlan_throwsIllegalStateException() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            plan.setDisbursementType(DisbursementType.SINGLE);
            given(disbursementPlanRepository.findById(1L)).willReturn(Optional.of(plan));

            assertThatThrownBy(() -> disbursementStageService.createStage(
                    1L, 1, new BigDecimal("2000.00"), "Milestone", LocalDate.now().plusDays(30)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SINGLE");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when stage number is zero")
        void zeroStageNumber_throwsIllegalArgumentException() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.findById(1L)).willReturn(Optional.of(plan));

            assertThatThrownBy(() -> disbursementStageService.createStage(
                    1L, 0, new BigDecimal("2000.00"), "Milestone", LocalDate.now().plusDays(30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stage number");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when stage amount is zero")
        void zeroAmount_throwsIllegalArgumentException() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.findById(1L)).willReturn(Optional.of(plan));

            assertThatThrownBy(() -> disbursementStageService.createStage(
                    1L, 1, BigDecimal.ZERO, "Milestone", LocalDate.now().plusDays(30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stage amount");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when milestone is blank")
        void blankMilestone_throwsIllegalArgumentException() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.findById(1L)).willReturn(Optional.of(plan));

            assertThatThrownBy(() -> disbursementStageService.createStage(
                    1L, 1, new BigDecimal("2000.00"), "   ", LocalDate.now().plusDays(30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Milestone");
        }

        @Test
        @DisplayName("throws IllegalStateException when stage number already exists")
        void duplicateStageNumber_throwsIllegalStateException() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.findById(1L)).willReturn(Optional.of(plan));
            given(disbursementStageRepository.findByDisbursementPlanIdAndStageNumber(1L, 1))
                    .willReturn(Optional.of(pendingStage(plan, new BigDecimal("2000.00"), 1)));

            assertThatThrownBy(() -> disbursementStageService.createStage(
                    1L, 1, new BigDecimal("2000.00"), "Milestone", LocalDate.now().plusDays(30)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Stage 1");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when stage amount exceeds plan total")
        void amountExceedsPlanTotal_throwsIllegalArgumentException() {
            BigDecimal planTotal = new BigDecimal("6000.00");
            DisbursementPlan plan = stagedPlan(planTotal);
            given(disbursementPlanRepository.findById(1L)).willReturn(Optional.of(plan));
            given(disbursementStageRepository.findByDisbursementPlanIdAndStageNumber(1L, 1))
                    .willReturn(Optional.empty());
            given(disbursementStageRepository.findByDisbursementPlanIdOrderByStageNumberAsc(1L))
                    .willReturn(List.of());

            assertThatThrownBy(() -> disbursementStageService.createStage(
                    1L, 1, new BigDecimal("7000.00"), "Milestone", LocalDate.now().plusDays(30)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sanctioned amount");
        }

        @Test
        @DisplayName("throws when cumulative stage amounts would exceed plan total")
        void cumulativeAmountExceedsTotal_throwsIllegalArgumentException() {
            BigDecimal planTotal = new BigDecimal("6000.00");
            DisbursementPlan plan = stagedPlan(planTotal);
            DisbursementStage existingStage = pendingStage(plan, new BigDecimal("5000.00"), 1);

            given(disbursementPlanRepository.findById(1L)).willReturn(Optional.of(plan));
            given(disbursementStageRepository.findByDisbursementPlanIdAndStageNumber(1L, 2))
                    .willReturn(Optional.empty());
            given(disbursementStageRepository.findByDisbursementPlanIdOrderByStageNumberAsc(1L))
                    .willReturn(List.of(existingStage));

            // 5000 existing + 2000 new = 7000 > 6000
            assertThatThrownBy(() -> disbursementStageService.createStage(
                    1L, 2, new BigDecimal("2000.00"), "Stage 2", LocalDate.now().plusDays(60)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sanctioned amount");
        }
    }

    // ── VerifyStage ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyStage()")
    class VerifyStage {

        @Test
        @DisplayName("transitions stage from PENDING to VERIFIED")
        void pendingStage_transitionsToVerified() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = pendingStage(plan, new BigDecimal("2000.00"), 1);
            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            DisbursementStage result = disbursementStageService.verifyStage(1L);

            assertThat(result.getStatus()).isEqualTo(DisbursementStageStatus.VERIFIED);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when stage is not found")
        void stageNotFound_throwsIllegalArgumentException() {
            given(disbursementStageRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> disbursementStageService.verifyStage(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("throws IllegalStateException when stage is already RELEASED")
        void releasedStage_throwsIllegalStateException() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = verifiedStage(plan, new BigDecimal("2000.00"));
            stage.setStatus(DisbursementStageStatus.RELEASED);
            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));

            assertThatThrownBy(() -> disbursementStageService.verifyStage(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("released");
        }
    }

    // ── ReleaseStage ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("releaseStage()")
    class ReleaseStage {

        @Test
        @DisplayName("transitions stage status to RELEASED")
        void verifiedStage_transitionsToReleased() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = verifiedStage(plan, new BigDecimal("2000.00"));

            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementPlanRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            DisbursementStage result = disbursementStageService.releaseStage(1L);

            assertThat(result.getStatus()).isEqualTo(DisbursementStageStatus.RELEASED);
        }

        @Test
        @DisplayName("sets releasedAt timestamp when stage is released")
        void verifiedStage_setsReleasedAt() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = verifiedStage(plan, new BigDecimal("2000.00"));

            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementPlanRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            DisbursementStage result = disbursementStageService.releaseStage(1L);

            assertThat(result.getReleasedAt()).isNotNull();
        }

        @Test
        @DisplayName("updates plan releasedAmount after partial release")
        void partialRelease_updatesReleasedAmountOnPlan() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = verifiedStage(plan, new BigDecimal("2000.00"));

            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementPlanRepository.save(any(DisbursementPlan.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            disbursementStageService.releaseStage(1L);

            assertThat(plan.getReleasedAmount()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("updates plan remainingAmount after partial release")
        void partialRelease_updatesRemainingAmountOnPlan() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = verifiedStage(plan, new BigDecimal("2000.00"));

            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementPlanRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            disbursementStageService.releaseStage(1L);

            assertThat(plan.getRemainingAmount()).isEqualByComparingTo("4000.00");
        }

        @Test
        @DisplayName("sets plan status to PARTIALLY_RELEASED when amount is not fully released")
        void partialRelease_planStatusIsPartiallyReleased() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = verifiedStage(plan, new BigDecimal("2000.00"));

            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementPlanRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            disbursementStageService.releaseStage(1L);

            assertThat(plan.getStatus()).isEqualTo(DisbursementStatus.PARTIALLY_RELEASED);
        }

        @Test
        @DisplayName("sets plan status to FULLY_RELEASED when entire amount is released")
        void fullRelease_planStatusIsFullyReleased() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = verifiedStage(plan, new BigDecimal("6000.00"));

            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementPlanRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            disbursementStageService.releaseStage(1L);

            assertThat(plan.getStatus()).isEqualTo(DisbursementStatus.FULLY_RELEASED);
        }

        @Test
        @DisplayName("throws IllegalStateException when releasing a PENDING (not yet verified) stage")
        void pendingStage_throwsIllegalStateException() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = pendingStage(plan, new BigDecimal("2000.00"), 1);

            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));

            assertThatThrownBy(() -> disbursementStageService.releaseStage(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("VERIFIED");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when stage is not found")
        void stageNotFound_throwsIllegalArgumentException() {
            given(disbursementStageRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> disbursementStageService.releaseStage(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("saves both the updated stage and the updated plan")
        void validRelease_savesBothPlanAndStage() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = verifiedStage(plan, new BigDecimal("6000.00"));

            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementPlanRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            disbursementStageService.releaseStage(1L);

            verify(disbursementPlanRepository).save(plan);
            verify(disbursementStageRepository).save(stage);
        }
    }

    // ── UpdateComplianceStatus ────────────────────────────────────────────────

    @Nested
    @DisplayName("updateComplianceStatus()")
    class UpdateComplianceStatus {

        @Test
        @DisplayName("updates compliance status to COMPLETED")
        void updateToCompleted_setsComplianceStatusCompleted() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = pendingStage(plan, new BigDecimal("2000.00"), 1);
            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            DisbursementStage result =
                    disbursementStageService.updateComplianceStatus(1L, ComplianceStatus.COMPLETED);

            assertThat(result.getComplianceStatus()).isEqualTo(ComplianceStatus.COMPLETED);
        }

        @Test
        @DisplayName("updates compliance status to NON_COMPLIANT")
        void updateToNonCompliant_setsComplianceStatusNonCompliant() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = pendingStage(plan, new BigDecimal("2000.00"), 1);
            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            DisbursementStage result =
                    disbursementStageService.updateComplianceStatus(1L, ComplianceStatus.NON_COMPLIANT);

            assertThat(result.getComplianceStatus()).isEqualTo(ComplianceStatus.NON_COMPLIANT);
        }

        @Test
        @DisplayName("can reset compliance status back to PENDING")
        void updateToPending_setsComplianceStatusPending() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = pendingStage(plan, new BigDecimal("2000.00"), 1);
            stage.setComplianceStatus(ComplianceStatus.COMPLETED);
            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));
            given(disbursementStageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            DisbursementStage result =
                    disbursementStageService.updateComplianceStatus(1L, ComplianceStatus.PENDING);

            assertThat(result.getComplianceStatus()).isEqualTo(ComplianceStatus.PENDING);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when compliance status is null")
        void nullComplianceStatus_throwsIllegalArgumentException() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = pendingStage(plan, new BigDecimal("2000.00"), 1);
            given(disbursementStageRepository.findById(1L)).willReturn(Optional.of(stage));

            assertThatThrownBy(() ->
                    disbursementStageService.updateComplianceStatus(1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Compliance status cannot be null");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when stage is not found")
        void stageNotFound_throwsIllegalArgumentException() {
            given(disbursementStageRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    disbursementStageService.updateComplianceStatus(99L, ComplianceStatus.COMPLETED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── isOverdue ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isOverdue()")
    class IsOverdue {

        @Test
        @DisplayName("returns false when dueDate is in the future")
        void futureDueDate_notOverdue() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = pendingStage(plan, new BigDecimal("2000.00"), 1);
            stage.setDueDate(LocalDate.now().plusDays(10));

            assertThat(disbursementStageService.isOverdue(stage)).isFalse();
        }

        @Test
        @DisplayName("returns true when dueDate is in the past and stage is not RELEASED")
        void pastDueDatePendingStage_isOverdue() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = pendingStage(plan, new BigDecimal("2000.00"), 1);
            stage.setDueDate(LocalDate.now().minusDays(5));

            assertThat(disbursementStageService.isOverdue(stage)).isTrue();
        }

        @Test
        @DisplayName("returns false when stage is RELEASED even if dueDate is past")
        void pastDueDateReleasedStage_notOverdue() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = pendingStage(plan, new BigDecimal("2000.00"), 1);
            stage.setDueDate(LocalDate.now().minusDays(5));
            stage.setStatus(DisbursementStageStatus.RELEASED);

            assertThat(disbursementStageService.isOverdue(stage)).isFalse();
        }

        @Test
        @DisplayName("returns false when dueDate is null")
        void nullDueDate_notOverdue() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage stage = pendingStage(plan, new BigDecimal("2000.00"), 1);
            stage.setDueDate(null);

            assertThat(disbursementStageService.isOverdue(stage)).isFalse();
        }
    }

    // ── GetStages ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStages()")
    class GetStages {

        @Test
        @DisplayName("returns all stages ordered by stage number")
        void existingPlan_returnsOrderedStages() {
            DisbursementPlan plan = stagedPlan(new BigDecimal("6000.00"));
            DisbursementStage s1 = pendingStage(plan, new BigDecimal("2000.00"), 1);
            DisbursementStage s2 = pendingStage(plan, new BigDecimal("2000.00"), 2);
            DisbursementStage s3 = pendingStage(plan, new BigDecimal("2000.00"), 3);

            given(disbursementPlanRepository.existsById(1L)).willReturn(true);
            given(disbursementStageRepository.findByDisbursementPlanIdOrderByStageNumberAsc(1L))
                    .willReturn(List.of(s1, s2, s3));

            List<DisbursementStage> result = disbursementStageService.getStages(1L);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getStageNumber()).isEqualTo(1);
            assertThat(result.get(2).getStageNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when plan is not found")
        void planNotFound_throwsIllegalArgumentException() {
            given(disbursementPlanRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> disbursementStageService.getStages(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }
    }
}

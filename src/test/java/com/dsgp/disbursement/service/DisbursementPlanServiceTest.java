package com.dsgp.disbursement.service;

import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.disbursement.entity.DisbursementPlan;
import com.dsgp.disbursement.entity.DisbursementStatus;
import com.dsgp.disbursement.entity.DisbursementType;
import com.dsgp.disbursement.repository.DisbursementPlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisbursementPlanService")
class DisbursementPlanServiceTest {

    @Mock
    private DisbursementPlanRepository disbursementPlanRepository;

    @InjectMocks
    private DisbursementPlanService disbursementPlanService;

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
        s.setSchemeName("PM-KISAN Samman Nidhi");
        return s;
    }

    private SchemeApplication approvedApplication(BigDecimal sanctionedAmount) {
        SchemeApplication app = SchemeApplication.builder()
                .beneficiary(beneficiary())
                .scheme(scheme())
                .applicationStatus("APPROVED")
                .sanctionedAmount(sanctionedAmount)
                .build();
        app.setId(10L);
        return app;
    }

    /**
     * Stubs the repository so no plan exists yet for application 10.
     * Call this only in tests that reach the duplicate-plan check
     * (i.e. tests where both the null-app and status guards pass).
     */
    private void stubNoDuplicatePlan() {
        given(disbursementPlanRepository.existsByApplication_Id(10L)).willReturn(false);
    }

    // ── CreatePlan ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createPlan()")
    class CreatePlan {

        // ── Happy-path tests (all reach the existsByApplication_Id call) ──────

        @Test
        @DisplayName("returns saved plan for a valid APPROVED application")
        void validApprovedApplication_returnsSavedPlan() {
            stubNoDuplicatePlan();
            SchemeApplication app = approvedApplication(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.save(any(DisbursementPlan.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DisbursementPlan result = disbursementPlanService.createPlan(app, DisbursementType.STAGED);

            assertThat(result).isNotNull();
            verify(disbursementPlanRepository).save(any(DisbursementPlan.class));
        }

        @Test
        @DisplayName("initialises totalAmount from sanctionedAmount")
        void validApplication_totalAmountSetFromSanctionedAmount() {
            stubNoDuplicatePlan();
            BigDecimal sanctioned = new BigDecimal("50000.00");
            SchemeApplication app = approvedApplication(sanctioned);
            given(disbursementPlanRepository.save(any(DisbursementPlan.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DisbursementPlan result = disbursementPlanService.createPlan(app, DisbursementType.STAGED);

            assertThat(result.getTotalAmount()).isEqualByComparingTo(sanctioned);
        }

        @Test
        @DisplayName("initialises releasedAmount to zero")
        void validApplication_releasedAmountIsZero() {
            stubNoDuplicatePlan();
            SchemeApplication app = approvedApplication(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.save(any(DisbursementPlan.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DisbursementPlan result = disbursementPlanService.createPlan(app, DisbursementType.STAGED);

            assertThat(result.getReleasedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("initialises remainingAmount equal to totalAmount")
        void validApplication_remainingAmountEqualsTotalAmount() {
            stubNoDuplicatePlan();
            BigDecimal sanctioned = new BigDecimal("6000.00");
            SchemeApplication app = approvedApplication(sanctioned);
            given(disbursementPlanRepository.save(any(DisbursementPlan.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DisbursementPlan result = disbursementPlanService.createPlan(app, DisbursementType.STAGED);

            assertThat(result.getRemainingAmount()).isEqualByComparingTo(sanctioned);
        }

        @Test
        @DisplayName("initial plan status is PENDING")
        void validApplication_initialStatusIsPending() {
            stubNoDuplicatePlan();
            SchemeApplication app = approvedApplication(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.save(any(DisbursementPlan.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DisbursementPlan result = disbursementPlanService.createPlan(app, DisbursementType.STAGED);

            assertThat(result.getStatus()).isEqualTo(DisbursementStatus.PENDING);
        }

        @Test
        @DisplayName("sets the disbursement type on the plan")
        void validApplication_setsCorrectDisbursementType() {
            stubNoDuplicatePlan();
            SchemeApplication app = approvedApplication(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.save(any(DisbursementPlan.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DisbursementPlan result = disbursementPlanService.createPlan(app, DisbursementType.STAGED);

            assertThat(result.getDisbursementType()).isEqualTo(DisbursementType.STAGED);
        }

        @Test
        @DisplayName("persists the plan via repository save")
        void validApplication_callsRepositorySave() {
            stubNoDuplicatePlan();
            SchemeApplication app = approvedApplication(new BigDecimal("6000.00"));
            given(disbursementPlanRepository.save(any(DisbursementPlan.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            disbursementPlanService.createPlan(app, DisbursementType.STAGED);

            verify(disbursementPlanRepository).save(any(DisbursementPlan.class));
        }

        // ── Guard / error tests (throw before existsByApplication_Id is called) ─

        @Test
        @DisplayName("throws IllegalArgumentException when application is null")
        void nullApplication_throwsIllegalArgumentException() {
            // Service throws immediately — existsByApplication_Id never called; no stub needed
            assertThatThrownBy(() -> disbursementPlanService.createPlan(null, DisbursementType.STAGED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Application cannot be null");
        }

        @Test
        @DisplayName("throws IllegalStateException when application is not APPROVED")
        void pendingApplication_throwsIllegalStateException() {
            SchemeApplication app = approvedApplication(new BigDecimal("6000.00"));
            app.setApplicationStatus("PENDING");

            assertThatThrownBy(() -> disbursementPlanService.createPlan(app, DisbursementType.STAGED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("approved application");
        }

        @Test
        @DisplayName("throws IllegalStateException when application status is REJECTED")
        void rejectedApplication_throwsIllegalStateException() {
            SchemeApplication app = approvedApplication(new BigDecimal("6000.00"));
            app.setApplicationStatus("REJECTED");

            assertThatThrownBy(() -> disbursementPlanService.createPlan(app, DisbursementType.STAGED))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when sanctionedAmount is null")
        void nullSanctionedAmount_throwsIllegalStateException() {
            SchemeApplication app = approvedApplication(null);

            assertThatThrownBy(() -> disbursementPlanService.createPlan(app, DisbursementType.STAGED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Sanctioned amount");
        }

        @Test
        @DisplayName("throws IllegalStateException when sanctionedAmount is zero")
        void zeroSanctionedAmount_throwsIllegalStateException() {
            SchemeApplication app = approvedApplication(BigDecimal.ZERO);

            assertThatThrownBy(() -> disbursementPlanService.createPlan(app, DisbursementType.STAGED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Sanctioned amount");
        }

        @Test
        @DisplayName("throws IllegalStateException when sanctionedAmount is negative")
        void negativeSanctionedAmount_throwsIllegalStateException() {
            SchemeApplication app = approvedApplication(new BigDecimal("-100.00"));

            assertThatThrownBy(() -> disbursementPlanService.createPlan(app, DisbursementType.STAGED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Sanctioned amount");
        }

        @Test
        @DisplayName("throws IllegalStateException when a plan already exists for the application")
        void duplicatePlan_throwsIllegalStateException() {
            SchemeApplication app = approvedApplication(new BigDecimal("6000.00"));
            // Explicitly override: plan already exists
            given(disbursementPlanRepository.existsByApplication_Id(10L)).willReturn(true);

            assertThatThrownBy(() -> disbursementPlanService.createPlan(app, DisbursementType.STAGED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already exists");
        }
    }
}

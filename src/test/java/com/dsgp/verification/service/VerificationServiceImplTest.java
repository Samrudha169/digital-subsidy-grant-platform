package com.dsgp.verification.service;

import com.dsgp.application.exception.ApplicationException;
import com.dsgp.beneficiary.entity.Beneficiary;
import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.application.entity.SchemeApplication;
import com.dsgp.beneficiary.repository.SchemeApplicationRepository;
import com.dsgp.eligibility.entity.EligibilityResult;
import com.dsgp.eligibility.entity.EligibilityStatus;
import com.dsgp.eligibility.repository.EligibilityResultRepository;
import com.dsgp.verification.dto.VerificationActionRequest;
import com.dsgp.verification.dto.VerificationStatusResponse;
import com.dsgp.verification.entity.VerificationRecord;
import com.dsgp.verification.exception.InvalidVerificationTransitionException;
import com.dsgp.verification.repository.VerificationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link VerificationServiceImpl}.
 *
 * <p>All repository interactions are mocked. Tests verify:
 * <ul>
 *   <li>State-machine transitions are enforced correctly.</li>
 *   <li>Eligibility guard prevents ineligible applications from entering verification.</li>
 *   <li>Invalid transitions throw {@link InvalidVerificationTransitionException}.</li>
 *   <li>Rejection requires non-empty remarks.</li>
 *   <li>Audit records are saved for every action.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationServiceImpl")
class VerificationServiceImplTest {

    @Mock private SchemeApplicationRepository  applicationRepository;
    @Mock private EligibilityResultRepository  eligibilityResultRepository;
    @Mock private VerificationRepository       verificationRepository;

    @InjectMocks
    private VerificationServiceImpl service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final Long    APP_ID         = 1L;
    private static final Integer BENEFICIARY_ID = 101;
    private static final Long    SCHEME_ID      = 1L;

    private Beneficiary beneficiary() {
        Beneficiary b = new Beneficiary();
        b.setId(BENEFICIARY_ID);
        b.setFullName("Ravi Kumar");
        return b;
    }

    private Scheme scheme() {
        Scheme s = new Scheme();
        s.setId(SCHEME_ID);
        s.setSchemeName("PM-KISAN Samman Nidhi");
        s.setGrantAmount(new BigDecimal("6000.00"));
        return s;
    }

    private SchemeApplication app(String status) {
        SchemeApplication a = SchemeApplication.builder()
                .beneficiary(beneficiary())
                .scheme(scheme())
                .applicationStatus(status)
                .build();
        // Reflectively set id via builder not possible without @Builder.Default on id field;
        // use a spy or simply use findById mock to return this instance.
        return a;
    }

    private EligibilityResult eligibleResult() {
        return EligibilityResult.builder()
                .beneficiaryId(BENEFICIARY_ID)
                .schemeId(SCHEME_ID)
                .schemeName("PM-KISAN Samman Nidhi")
                .totalScore(80)
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private EligibilityResult ineligibleResult() {
        return EligibilityResult.builder()
                .beneficiaryId(BENEFICIARY_ID)
                .schemeId(SCHEME_ID)
                .schemeName("PM-KISAN Samman Nidhi")
                .totalScore(40)
                .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private VerificationActionRequest req(String by, String remarks) {
        VerificationActionRequest r = new VerificationActionRequest();
        r.setPerformedBy(by);
        r.setRemarks(remarks);
        return r;
    }

    // ════════════════════════════════════════════════════════════════════════
    // startVerification
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("startVerification")
    class StartVerification {

        @Test
        @DisplayName("eligible PENDING application moves to UNDER_REVIEW")
        void start_eligible_movesToUnderReview() {
            SchemeApplication pending = app("PENDING");
            given(applicationRepository.findById(APP_ID)).willReturn(Optional.of(pending));
            given(eligibilityResultRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.of(eligibleResult()));
            given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(any()))
                    .willReturn(List.of());

            VerificationStatusResponse resp = service.startVerification(APP_ID, req("field_officer_1", null));

            assertThat(resp.getApplicationStatus()).isEqualTo("UNDER_REVIEW");
            then(verificationRepository).should().save(any(VerificationRecord.class));
        }

        @Test
        @DisplayName("INELIGIBLE application cannot start verification")
        void start_ineligible_throws() {
            SchemeApplication pending = app("PENDING");
            given(applicationRepository.findById(APP_ID)).willReturn(Optional.of(pending));
            given(eligibilityResultRepository.findByBeneficiaryIdAndSchemeId(BENEFICIARY_ID, SCHEME_ID))
                    .willReturn(Optional.of(ineligibleResult()));

            assertThatThrownBy(() -> service.startVerification(APP_ID, req("officer", null)))
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("INELIGIBLE");
        }

        @Test
        @DisplayName("non-PENDING application cannot start verification")
        void start_nonPending_throws() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("UNDER_REVIEW")));

            assertThatThrownBy(() -> service.startVerification(APP_ID, req("officer", null)))
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("application not found throws ApplicationException")
        void start_notFound_throws() {
            given(applicationRepository.findById(APP_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.startVerification(APP_ID, req("officer", null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Field Officer
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Field Officer actions")
    class FieldOfficer {

        @Test
        @DisplayName("approve: UNDER_REVIEW → FIELD_APPROVED")
        void field_approve_movesToFieldApproved() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("UNDER_REVIEW")));
            given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(any()))
                    .willReturn(List.of());

            VerificationStatusResponse resp =
                    service.approveAtField(APP_ID, req("field_officer_1", "Documents confirmed."));

            assertThat(resp.getApplicationStatus()).isEqualTo("FIELD_APPROVED");
        }

        @Test
        @DisplayName("reject: UNDER_REVIEW → REJECTED (with remarks)")
        void field_reject_movesToRejected() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("UNDER_REVIEW")));
            given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(any()))
                    .willReturn(List.of());

            VerificationStatusResponse resp =
                    service.rejectAtField(APP_ID, req("field_officer_1", "Fraudulent documents."));

            assertThat(resp.getApplicationStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("reject without remarks throws exception")
        void field_reject_noRemarks_throws() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("UNDER_REVIEW")));

            assertThatThrownBy(() -> service.rejectAtField(APP_ID, req("officer", "")))
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("remarks");
        }

        @Test
        @DisplayName("escalate: UNDER_REVIEW → ESCALATED")
        void field_escalate_movesToEscalated() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("UNDER_REVIEW")));
            given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(any()))
                    .willReturn(List.of());

            VerificationStatusResponse resp =
                    service.escalateAtField(APP_ID, req("field_officer_1", "Needs district review."));

            assertThat(resp.getApplicationStatus()).isEqualTo("ESCALATED");
        }

        @Test
        @DisplayName("approve on non-UNDER_REVIEW throws invalid transition")
        void field_approve_wrongStatus_throws() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("PENDING")));

            assertThatThrownBy(() -> service.approveAtField(APP_ID, req("officer", null)))
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("UNDER_REVIEW");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // District Officer
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("District Officer actions")
    class DistrictOfficer {

        @Test
        @DisplayName("approve: ESCALATED → DISTRICT_APPROVED")
        void district_approve_movesToDistrictApproved() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("ESCALATED")));
            given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(any()))
                    .willReturn(List.of());

            VerificationStatusResponse resp =
                    service.approveAtDistrict(APP_ID, req("district_officer_1", null));

            assertThat(resp.getApplicationStatus()).isEqualTo("DISTRICT_APPROVED");
        }

        @Test
        @DisplayName("reject: ESCALATED → REJECTED (with remarks)")
        void district_reject_movesToRejected() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("ESCALATED")));
            given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(any()))
                    .willReturn(List.of());

            VerificationStatusResponse resp =
                    service.rejectAtDistrict(APP_ID, req("district_officer_1", "Does not qualify."));

            assertThat(resp.getApplicationStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("district reject without remarks throws")
        void district_reject_noRemarks_throws() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("ESCALATED")));

            assertThatThrownBy(() -> service.rejectAtDistrict(APP_ID, req("officer", null)))
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("remarks");
        }

        @Test
        @DisplayName("district action on non-ESCALATED application throws")
        void district_wrongStatus_throws() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("UNDER_REVIEW")));

            assertThatThrownBy(() -> service.approveAtDistrict(APP_ID, req("officer", null)))
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("ESCALATED");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Finance Approver
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Finance Approver actions")
    class FinanceApprover {

        @Test
        @DisplayName("approve from FIELD_APPROVED → APPROVED")
        void finance_approve_fromFieldApproved() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("FIELD_APPROVED")));
            given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(any()))
                    .willReturn(List.of());

            VerificationStatusResponse resp =
                    service.approveAtFinance(APP_ID, req("finance_approver_1", null));

            assertThat(resp.getApplicationStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("approve from DISTRICT_APPROVED → APPROVED")
        void finance_approve_fromDistrictApproved() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("DISTRICT_APPROVED")));
            given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(any()))
                    .willReturn(List.of());

            VerificationStatusResponse resp =
                    service.approveAtFinance(APP_ID, req("finance_approver_1", null));

            assertThat(resp.getApplicationStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("reject from FIELD_APPROVED → REJECTED")
        void finance_reject_fromFieldApproved() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("FIELD_APPROVED")));
            given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(any()))
                    .willReturn(List.of());

            VerificationStatusResponse resp =
                    service.rejectAtFinance(APP_ID, req("finance_approver_1", "Insufficient supporting docs."));

            assertThat(resp.getApplicationStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("finance reject without remarks throws")
        void finance_reject_noRemarks_throws() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("FIELD_APPROVED")));

            assertThatThrownBy(() -> service.rejectAtFinance(APP_ID, req("officer", "")))
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("remarks");
        }

        @Test
        @DisplayName("finance action on UNDER_REVIEW throws invalid transition")
        void finance_wrongStatus_throws() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("UNDER_REVIEW")));

            assertThatThrownBy(() -> service.approveAtFinance(APP_ID, req("officer", null)))
                    .isInstanceOf(InvalidVerificationTransitionException.class)
                    .hasMessageContaining("FIELD_APPROVED");
        }

        @Test
        @DisplayName("finance action on terminal APPROVED throws invalid transition")
        void finance_alreadyApproved_throws() {
            given(applicationRepository.findById(APP_ID))
                    .willReturn(Optional.of(app("APPROVED")));

            assertThatThrownBy(() -> service.approveAtFinance(APP_ID, req("officer", null)))
                    .isInstanceOf(InvalidVerificationTransitionException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getStatus
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("returns response with application details and history")
        void getStatus_returnsCorrectResponse() {
            SchemeApplication a = app("PENDING");
            given(applicationRepository.findById(APP_ID)).willReturn(Optional.of(a));
            given(verificationRepository.findBySchemeApplicationIdOrderByPerformedAtAsc(any()))
                    .willReturn(List.of());

            VerificationStatusResponse resp = service.getStatus(APP_ID);

            assertThat(resp.getBeneficiaryId()).isEqualTo(BENEFICIARY_ID);
            assertThat(resp.getSchemeId()).isEqualTo(SCHEME_ID);
            assertThat(resp.getApplicationStatus()).isEqualTo("PENDING");
            assertThat(resp.getHistory()).isEmpty();
        }
    }
}

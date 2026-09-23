package com.dsgp.verification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for any verification stage action (approve / reject / escalate).
 *
 * <p>Used for all action endpoints:
 * <ul>
 *   <li>{@code POST /verification/applications/{id}/field-approve}</li>
 *   <li>{@code POST /verification/applications/{id}/field-reject}</li>
 *   <li>{@code POST /verification/applications/{id}/escalate}</li>
 *   <li>{@code POST /verification/applications/{id}/district-approve}</li>
 *   <li>{@code POST /verification/applications/{id}/district-reject}</li>
 *   <li>{@code POST /verification/applications/{id}/finance-approve}</li>
 *   <li>{@code POST /verification/applications/{id}/finance-reject}</li>
 * </ul>
 *
 * <p>The {@code sanctionedAmount} field is only required for the
 * {@code finance-approve} action; it is ignored (and must be null) for all
 * other actions. Validation of the value itself (positive, ≤ grantAmount) is
 * enforced in {@link com.dsgp.verification.service.VerificationServiceImpl}.
 */
@Data
public class VerificationActionRequest {

    /**
     * Username or identifier of the officer performing this action.
     * Full RBAC is implemented in Milestone 5; for Milestone 2 this is a
     * free-text field in the request body.
     */
    @NotBlank(message = "performedBy is required — provide the officer's username")
    private String performedBy;

    /**
     * Optional remarks / reason for the decision.
     * Mandatory for REJECT actions (enforced at the service layer).
     * Recommended for ESCALATE actions.
     */
    private String remarks;

    /**
     * The amount (in ₹) sanctioned by the Finance Officer for this application.
     *
     * <p>Required only for the {@code finance-approve} action.
     * Must be &gt; 0 and must not exceed the scheme's {@code grantAmount}.
     * The service layer enforces these rules; a {@code null} value here will
     * cause an {@link com.dsgp.verification.exception.InvalidVerificationTransitionException}
     * to be thrown during Finance approval.
     *
     * <p>Ignored for all other verification actions.
     */
    private BigDecimal sanctionedAmount;
}

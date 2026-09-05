package com.dsgp.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for any verification stage action (approve / reject / escalate).
 *
 * <p>Used for all three action endpoints:
 * <ul>
 *   <li>{@code POST /verification/applications/{id}/field-approve}</li>
 *   <li>{@code POST /verification/applications/{id}/field-reject}</li>
 *   <li>{@code POST /verification/applications/{id}/escalate}</li>
 *   <li>{@code POST /verification/applications/{id}/district-approve}</li>
 *   <li>{@code POST /verification/applications/{id}/district-reject}</li>
 *   <li>{@code POST /verification/applications/{id}/finance-approve}</li>
 *   <li>{@code POST /verification/applications/{id}/finance-reject}</li>
 * </ul>
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
}

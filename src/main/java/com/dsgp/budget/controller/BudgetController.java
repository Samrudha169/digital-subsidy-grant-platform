package com.dsgp.budget.controller;

import com.dsgp.budget.dto.BudgetRequest;
import com.dsgp.budget.dto.BudgetResponse;
import com.dsgp.budget.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/budgets")
@CrossOrigin
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAllBudgets() {

        return ResponseEntity.ok(
                budgetService.getAllBudgets()
        );
    }

    @GetMapping("/active")
    public ResponseEntity<List<BudgetResponse>> getActiveBudgets() {

        return ResponseEntity.ok(
                budgetService.getActiveBudgets()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                budgetService.getBudgetById(id)
        );
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody BudgetRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(budgetService.createBudget(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request
    ) {

        return ResponseEntity.ok(
                budgetService.updateBudget(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateBudget(
            @PathVariable Long id
    ) {

        budgetService.deactivateBudget(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<BudgetResponse> reactivateBudget(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                budgetService.reactivateBudget(id)
        );
    }
}
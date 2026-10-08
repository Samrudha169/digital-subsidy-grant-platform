package com.dsgp.budget.service;

import com.dsgp.budget.dto.BudgetRequest;
import com.dsgp.budget.dto.BudgetResponse;

import java.util.List;

public interface BudgetService {

    List<BudgetResponse> getAllBudgets();

    List<BudgetResponse> getActiveBudgets();

    BudgetResponse getBudgetById(Long id);

    BudgetResponse createBudget(BudgetRequest request);

    BudgetResponse updateBudget(Long id, BudgetRequest request);

    void deactivateBudget(Long id);

    BudgetResponse reactivateBudget(Long id);
}
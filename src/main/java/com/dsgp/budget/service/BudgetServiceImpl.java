package com.dsgp.budget.service;

import com.dsgp.beneficiary.entity.Scheme;
import com.dsgp.beneficiary.repository.SchemeRepository;
import com.dsgp.budget.dto.BudgetRequest;
import com.dsgp.budget.dto.BudgetResponse;
import com.dsgp.budget.entity.BudgetAllocation;
import com.dsgp.budget.repository.BudgetAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BudgetServiceImpl implements BudgetService {

    private final BudgetAllocationRepository budgetRepository;
    private final SchemeRepository schemeRepository;

    public BudgetServiceImpl(
            BudgetAllocationRepository budgetRepository,
            SchemeRepository schemeRepository
    ) {
        this.budgetRepository = budgetRepository;
        this.schemeRepository = schemeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgets() {
        return budgetRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getActiveBudgets() {
        return budgetRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id) {

        BudgetAllocation budget = budgetRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Budget allocation not found with ID: " + id)
                );

        return toResponse(budget);
    }

    @Override
    public BudgetResponse createBudget(BudgetRequest request) {

        Scheme scheme = schemeRepository.findById(request.getSchemeId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Scheme not found with ID: " + request.getSchemeId()
                        )
                );

        if (Boolean.FALSE.equals(scheme.getActive())) {
            throw new RuntimeException(
                    "Cannot create budget for an inactive scheme"
            );
        }

        String region = request.getRegion().trim();

        if (budgetRepository.existsBySchemeIdAndRegionIgnoreCase(
                request.getSchemeId(),
                region
        )) {
            throw new RuntimeException(
                    "Budget allocation already exists for this scheme and region"
            );
        }

        validateAmount(request.getAllocatedAmount());

        BudgetAllocation budget = new BudgetAllocation();

        budget.setScheme(scheme);
        budget.setRegion(region);
        budget.setAllocatedAmount(request.getAllocatedAmount());
        budget.setUsedAmount(BigDecimal.ZERO);
        budget.setActive(true);

        BudgetAllocation savedBudget = budgetRepository.save(budget);

        return toResponse(savedBudget);
    }

    @Override
    public BudgetResponse updateBudget(
            Long id,
            BudgetRequest request
    ) {

        BudgetAllocation budget = budgetRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Budget allocation not found with ID: " + id
                        )
                );

        Scheme scheme = schemeRepository.findById(request.getSchemeId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Scheme not found with ID: " + request.getSchemeId()
                        )
                );

        if (Boolean.FALSE.equals(scheme.getActive())) {
            throw new RuntimeException(
                    "Cannot assign budget to an inactive scheme"
            );
        }

        String region = request.getRegion().trim();

        if (budgetRepository.existsBySchemeIdAndRegionIgnoreCaseAndIdNot(
                request.getSchemeId(),
                region,
                id
        )) {
            throw new RuntimeException(
                    "Another budget allocation already exists for this scheme and region"
            );
        }

        validateAmount(request.getAllocatedAmount());

        BigDecimal usedAmount = budget.getUsedAmount() != null
                ? budget.getUsedAmount()
                : BigDecimal.ZERO;

        if (request.getAllocatedAmount().compareTo(usedAmount) < 0) {
            throw new RuntimeException(
                    "Allocated amount cannot be less than the amount already used"
            );
        }

        budget.setScheme(scheme);
        budget.setRegion(region);
        budget.setAllocatedAmount(request.getAllocatedAmount());

        BudgetAllocation updatedBudget = budgetRepository.save(budget);

        return toResponse(updatedBudget);
    }

    @Override
    public void deactivateBudget(Long id) {

        BudgetAllocation budget = budgetRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Budget allocation not found with ID: " + id
                        )
                );

        budget.setActive(false);

        budgetRepository.save(budget);
    }

    @Override
    public BudgetResponse reactivateBudget(Long id) {

        BudgetAllocation budget = budgetRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Budget allocation not found with ID: " + id
                        )
                );

        if (Boolean.TRUE.equals(budget.getActive())) {
            return toResponse(budget);
        }

        budget.setActive(true);

        BudgetAllocation updatedBudget = budgetRepository.save(budget);

        return toResponse(updatedBudget);
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Allocated amount must be greater than zero"
            );
        }
    }

    private BudgetResponse toResponse(BudgetAllocation budget) {

        BudgetResponse response = new BudgetResponse();

        response.setId(budget.getId());

        if (budget.getScheme() != null) {
            response.setSchemeId(budget.getScheme().getId());
            response.setSchemeName(budget.getScheme().getSchemeName());
        }

        response.setRegion(budget.getRegion());
        response.setAllocatedAmount(budget.getAllocatedAmount());

        BigDecimal usedAmount = budget.getUsedAmount() != null
                ? budget.getUsedAmount()
                : BigDecimal.ZERO;

        response.setUsedAmount(usedAmount);

        response.setRemainingAmount(
                budget.getAllocatedAmount().subtract(usedAmount)
        );

        response.setActive(budget.getActive());
        response.setCreatedAt(budget.getCreatedAt());
        response.setUpdatedAt(budget.getUpdatedAt());

        return response;
    }
}
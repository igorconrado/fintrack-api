package com.fintrack.budget;

import com.fintrack.auth.User;
import com.fintrack.budget.dto.BudgetRequest;
import com.fintrack.budget.dto.BudgetResponse;
import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.config.AuthHelper;
import com.fintrack.transaction.TransactionRepository;
import com.fintrack.transaction.TransactionType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AuthHelper authHelper;

    public List<BudgetResponse> getByPeriod(Integer month, Integer year) {
        User user = authHelper.getCurrentUser();
        return budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year).stream()
                .map(budget -> toResponse(budget, user.getId()))
                .toList();
    }

    public BudgetResponse create(BudgetRequest request) {
        User user = authHelper.getCurrentUser();

        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                user.getId(), request.getCategoryId(), request.getMonth(), request.getYear()
        ).ifPresent(b -> {
            throw new IllegalArgumentException("Budget already exists for this category and period");
        });

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        Budget budget = Budget.builder()
                .category(category)
                .month(request.getMonth())
                .year(request.getYear())
                .limitAmount(request.getLimitAmount())
                .user(user)
                .build();

        budgetRepository.save(budget);
        return toResponse(budget, user.getId());
    }

    public BudgetResponse update(UUID id, BudgetRequest request) {
        User user = authHelper.getCurrentUser();
        Budget budget = budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
        budget.setLimitAmount(request.getLimitAmount());
        budgetRepository.save(budget);
        return toResponse(budget, user.getId());
    }

    public void delete(UUID id) {
        User user = authHelper.getCurrentUser();
        Budget budget = budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    private BudgetResponse toResponse(Budget budget, UUID userId) {
        BigDecimal spent = transactionRepository.sumByTypeAndPeriod(
                userId, TransactionType.EXPENSE, budget.getCategory().getId(),
                budget.getMonth(), budget.getYear());

        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        BigDecimal remaining = budget.getLimitAmount().subtract(spent);
        double percentage = budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
                ? spent.multiply(BigDecimal.valueOf(100))
                    .divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP)
                    .doubleValue()
                : 0;

        String status;
        if (percentage >= 100) {
            status = "EXCEEDED";
        } else if (percentage >= 80) {
            status = "WARNING";
        } else {
            status = "ON_TRACK";
        }

        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getName())
                .month(budget.getMonth())
                .year(budget.getYear())
                .limitAmount(budget.getLimitAmount())
                .spentAmount(spent)
                .remainingAmount(remaining)
                .percentageUsed(percentage)
                .status(status)
                .build();
    }
}

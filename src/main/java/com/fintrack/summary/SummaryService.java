package com.fintrack.summary;

import com.fintrack.auth.User;
import com.fintrack.config.AuthHelper;
import com.fintrack.summary.dto.CategoryBreakdown;
import com.fintrack.summary.dto.MonthlySummaryResponse;
import com.fintrack.summary.dto.MonthlySummaryResponse.CategoryBreakdownDto;
import com.fintrack.transaction.TransactionRepository;
import com.fintrack.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TransactionRepository transactionRepository;
    private final AuthHelper authHelper;

    public MonthlySummaryResponse getMonthlySummary(Integer month, Integer year) {
        User user = authHelper.getCurrentUser();

        BigDecimal totalIncome = transactionRepository.sumByTypeAndPeriod(
                user.getId(), TransactionType.INCOME, null, month, year);
        BigDecimal totalExpenses = transactionRepository.sumByTypeAndPeriod(
                user.getId(), TransactionType.EXPENSE, null, month, year);

        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal balance = totalIncome.subtract(totalExpenses);
        double savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? balance.multiply(BigDecimal.valueOf(100))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP)
                    .doubleValue()
                : 0;

        List<CategoryBreakdown> incomeBreakdown = transactionRepository.getCategoryBreakdown(
                user.getId(), TransactionType.INCOME, month, year);
        List<CategoryBreakdown> expenseBreakdown = transactionRepository.getCategoryBreakdown(
                user.getId(), TransactionType.EXPENSE, month, year);

        BigDecimal finalTotalIncome = totalIncome;
        BigDecimal finalTotalExpenses = totalExpenses;

        List<CategoryBreakdownDto> incomeByCategory = incomeBreakdown.stream()
                .map(cb -> toCategoryDto(cb, finalTotalIncome))
                .toList();

        List<CategoryBreakdownDto> expensesByCategory = expenseBreakdown.stream()
                .map(cb -> toCategoryDto(cb, finalTotalExpenses))
                .toList();

        return MonthlySummaryResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .balance(balance)
                .savingsRate(savingsRate)
                .incomeByCategory(incomeByCategory)
                .expensesByCategory(expensesByCategory)
                .build();
    }

    private CategoryBreakdownDto toCategoryDto(CategoryBreakdown cb, BigDecimal total) {
        double percentage = total.compareTo(BigDecimal.ZERO) > 0
                ? cb.getTotal().multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP)
                    .doubleValue()
                : 0;

        return CategoryBreakdownDto.builder()
                .categoryName(cb.getCategoryName())
                .total(cb.getTotal())
                .percentage(percentage)
                .build();
    }
}

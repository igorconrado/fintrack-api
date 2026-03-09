package com.fintrack.budget;

import com.fintrack.auth.User;
import com.fintrack.budget.dto.BudgetRequest;
import com.fintrack.budget.dto.BudgetResponse;
import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.config.AuthHelper;
import com.fintrack.transaction.TransactionRepository;
import com.fintrack.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthHelper authHelper;

    @InjectMocks
    private BudgetService budgetService;

    private User user;
    private UUID userId;
    private Category category;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("test@fintrack.com").name("Test").build();

        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Food")
                .build();
    }

    @Test
    void create_shouldSaveBudget_whenNoDuplicate() {
        BudgetRequest request = new BudgetRequest(category.getId(), 3, 2026, BigDecimal.valueOf(500));

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(userId, category.getId(), 3, 2026))
                .thenReturn(Optional.empty());
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.sumByTypeAndPeriod(userId, TransactionType.EXPENSE, category.getId(), 3, 2026))
                .thenReturn(BigDecimal.valueOf(200));

        BudgetResponse result = budgetService.create(request);

        assertThat(result.getCategoryName()).isEqualTo("Food");
        assertThat(result.getLimitAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.getSpentAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(result.getStatus()).isEqualTo("ON_TRACK");
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void create_shouldThrowException_whenDuplicateExists() {
        BudgetRequest request = new BudgetRequest(category.getId(), 3, 2026, BigDecimal.valueOf(500));
        Budget existing = Budget.builder().id(UUID.randomUUID()).build();

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(userId, category.getId(), 3, 2026))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> budgetService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Budget already exists for this category and period");

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void getByPeriod_shouldReturnBudgetsWithSpentAmounts() {
        Budget budget = Budget.builder()
                .id(UUID.randomUUID())
                .category(category)
                .month(3)
                .year(2026)
                .limitAmount(BigDecimal.valueOf(1000))
                .user(user)
                .build();

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(budgetRepository.findByUserIdAndMonthAndYear(userId, 3, 2026)).thenReturn(List.of(budget));
        when(transactionRepository.sumByTypeAndPeriod(userId, TransactionType.EXPENSE, category.getId(), 3, 2026))
                .thenReturn(BigDecimal.valueOf(850));

        List<BudgetResponse> result = budgetService.getByPeriod(3, 2026);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSpentAmount()).isEqualByComparingTo(BigDecimal.valueOf(850));
        assertThat(result.get(0).getPercentageUsed()).isEqualTo(85.0);
        assertThat(result.get(0).getStatus()).isEqualTo("WARNING");
    }
}

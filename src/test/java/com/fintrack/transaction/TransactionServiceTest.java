package com.fintrack.transaction;

import com.fintrack.account.Account;
import com.fintrack.account.AccountRepository;
import com.fintrack.account.AccountType;
import com.fintrack.auth.User;
import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.config.AuthHelper;
import com.fintrack.transaction.dto.TransactionRequest;
import com.fintrack.transaction.dto.TransactionResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthHelper authHelper;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private UUID userId;
    private Account account;
    private Category category;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("test@fintrack.com").name("Test").build();

        account = Account.builder()
                .id(UUID.randomUUID())
                .name("Checking")
                .type(AccountType.CHECKING)
                .balance(BigDecimal.valueOf(1000))
                .user(user)
                .build();

        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Salary")
                .build();
    }

    @Test
    void create_shouldSaveTransaction_andIncreaseBalance_forIncome() {
        TransactionRequest request = new TransactionRequest(
                "Salary", BigDecimal.valueOf(3000), TransactionType.INCOME,
                LocalDate.of(2026, 3, 1), account.getId(), category.getId());

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(accountRepository.findByIdAndUserId(account.getId(), userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserIdIsNull(category.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse result = transactionService.create(request);

        assertThat(result.getDescription()).isEqualTo("Salary");
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        verify(accountRepository).save(account);
    }

    @Test
    void create_shouldSaveTransaction_andDecreaseBalance_forExpense() {
        TransactionRequest request = new TransactionRequest(
                "Rent", BigDecimal.valueOf(500), TransactionType.EXPENSE,
                LocalDate.of(2026, 3, 5), account.getId(), category.getId());

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(accountRepository.findByIdAndUserId(account.getId(), userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserIdIsNull(category.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse result = transactionService.create(request);

        assertThat(result.getDescription()).isEqualTo("Rent");
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(500));
        verify(accountRepository).save(account);
    }

    @Test
    void delete_shouldReverseBalanceEffect_onDeletion() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .description("Expense")
                .amount(BigDecimal.valueOf(200))
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2026, 3, 10))
                .account(account)
                .category(category)
                .user(user)
                .build();

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(transaction));

        transactionService.delete(transactionId);

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1200));
        verify(accountRepository).save(account);
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void getById_shouldThrowException_whenNotFound() {
        UUID transactionId = UUID.randomUUID();

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(transactionId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Transaction not found");
    }
}

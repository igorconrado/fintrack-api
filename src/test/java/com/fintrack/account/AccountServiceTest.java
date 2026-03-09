package com.fintrack.account;

import com.fintrack.account.dto.AccountRequest;
import com.fintrack.account.dto.AccountResponse;
import com.fintrack.auth.User;
import com.fintrack.config.AuthHelper;
import jakarta.persistence.EntityNotFoundException;
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
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuthHelper authHelper;

    @InjectMocks
    private AccountService accountService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("test@fintrack.com").name("Test").build();
    }

    @Test
    void getAll_shouldReturnUserAccounts() {
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .name("Checking")
                .type(AccountType.CHECKING)
                .balance(BigDecimal.valueOf(1000))
                .user(user)
                .build();

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(accountRepository.findByUserId(userId)).thenReturn(List.of(account));

        List<AccountResponse> result = accountService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Checking");
        assertThat(result.get(0).getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(accountRepository).findByUserId(userId);
    }

    @Test
    void getById_shouldReturnAccount_whenExists() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder()
                .id(accountId)
                .name("Savings")
                .type(AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(5000))
                .user(user)
                .build();

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));

        AccountResponse result = accountService.getById(accountId);

        assertThat(result.getId()).isEqualTo(accountId);
        assertThat(result.getName()).isEqualTo("Savings");
    }

    @Test
    void getById_shouldThrowException_whenNotFound() {
        UUID accountId = UUID.randomUUID();

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getById(accountId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Account not found");
    }

    @Test
    void create_shouldSaveAndReturnAccount() {
        AccountRequest request = new AccountRequest("New Account", AccountType.CHECKING, BigDecimal.valueOf(500));

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse result = accountService.create(request);

        assertThat(result.getName()).isEqualTo("New Account");
        assertThat(result.getType()).isEqualTo(AccountType.CHECKING);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(500));
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void delete_shouldDeleteAccount_whenBelongsToUser() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder()
                .id(accountId)
                .name("To Delete")
                .type(AccountType.CASH)
                .balance(BigDecimal.ZERO)
                .user(user)
                .build();

        when(authHelper.getCurrentUser()).thenReturn(user);
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));

        accountService.delete(accountId);

        verify(accountRepository).delete(account);
    }
}

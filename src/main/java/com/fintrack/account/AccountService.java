package com.fintrack.account;

import com.fintrack.account.dto.AccountRequest;
import com.fintrack.account.dto.AccountResponse;
import com.fintrack.auth.User;
import com.fintrack.config.AuthHelper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuthHelper authHelper;

    public List<AccountResponse> getAll() {
        User user = authHelper.getCurrentUser();
        return accountRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse getById(UUID id) {
        User user = authHelper.getCurrentUser();
        Account account = accountRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        return toResponse(account);
    }

    public AccountResponse create(AccountRequest request) {
        User user = authHelper.getCurrentUser();
        Account account = Account.builder()
                .name(request.getName())
                .type(request.getType())
                .balance(request.getInitialBalance())
                .user(user)
                .build();
        accountRepository.save(account);
        return toResponse(account);
    }

    public AccountResponse update(UUID id, AccountRequest request) {
        User user = authHelper.getCurrentUser();
        Account account = accountRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        account.setName(request.getName());
        account.setType(request.getType());
        accountRepository.save(account);
        return toResponse(account);
    }

    public void delete(UUID id) {
        User user = authHelper.getCurrentUser();
        Account account = accountRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        accountRepository.delete(account);
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}

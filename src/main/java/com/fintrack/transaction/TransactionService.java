package com.fintrack.transaction;

import com.fintrack.account.Account;
import com.fintrack.account.AccountRepository;
import com.fintrack.auth.User;
import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.config.AuthHelper;
import com.fintrack.transaction.dto.TransactionRequest;
import com.fintrack.transaction.dto.TransactionResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final AuthHelper authHelper;

    public Page<TransactionResponse> getAll(TransactionType type, UUID categoryId, UUID accountId,
                                            LocalDate startDate, LocalDate endDate, int page, int size) {
        User user = authHelper.getCurrentUser();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        return transactionRepository.findWithFilters(user.getId(), type, categoryId, accountId,
                startDate, endDate, pageRequest).map(this::toResponse);
    }

    public TransactionResponse getById(UUID id) {
        User user = authHelper.getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        User user = authHelper.getCurrentUser();

        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        Category category = resolveCategory(request.getCategoryId(), user);

        Transaction transaction = Transaction.builder()
                .description(request.getDescription())
                .amount(request.getAmount())
                .type(request.getType())
                .date(request.getDate())
                .account(account)
                .category(category)
                .user(user)
                .build();

        transactionRepository.save(transaction);
        applyBalanceEffect(account, request.getAmount(), request.getType());
        accountRepository.save(account);

        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse update(UUID id, TransactionRequest request) {
        User user = authHelper.getCurrentUser();

        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        Account oldAccount = transaction.getAccount();
        reverseBalanceEffect(oldAccount, transaction.getAmount(), transaction.getType());
        accountRepository.save(oldAccount);

        Account newAccount = accountRepository.findByIdAndUserId(request.getAccountId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        Category category = resolveCategory(request.getCategoryId(), user);

        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDate(request.getDate());
        transaction.setAccount(newAccount);
        transaction.setCategory(category);

        transactionRepository.save(transaction);
        applyBalanceEffect(newAccount, request.getAmount(), request.getType());
        accountRepository.save(newAccount);

        return toResponse(transaction);
    }

    @Transactional
    public void delete(UUID id) {
        User user = authHelper.getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        Account account = transaction.getAccount();
        reverseBalanceEffect(account, transaction.getAmount(), transaction.getType());
        accountRepository.save(account);

        transactionRepository.delete(transaction);
    }

    private Category resolveCategory(UUID categoryId, User user) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findByIdAndUserIdIsNull(categoryId)
                .or(() -> categoryRepository.findByIdAndUserId(categoryId, user.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    private void applyBalanceEffect(Account account, BigDecimal amount, TransactionType type) {
        if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }
    }

    private void reverseBalanceEffect(Account account, BigDecimal amount, TransactionType type) {
        if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(amount));
        } else {
            account.setBalance(account.getBalance().add(amount));
        }
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .description(transaction.getDescription())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .date(transaction.getDate())
                .createdAt(transaction.getCreatedAt())
                .accountId(transaction.getAccount().getId())
                .accountName(transaction.getAccount().getName())
                .categoryId(transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .categoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
                .build();
    }
}

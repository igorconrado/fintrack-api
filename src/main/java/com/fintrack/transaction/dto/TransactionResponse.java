package com.fintrack.transaction.dto;

import com.fintrack.transaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID id;
    private String description;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDate date;
    private LocalDateTime createdAt;
    private UUID accountId;
    private String accountName;
    private UUID categoryId;
    private String categoryName;
}

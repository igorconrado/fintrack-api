package com.fintrack.account.dto;

import com.fintrack.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    @NotBlank
    private String name;

    @NotNull
    private AccountType type;

    private BigDecimal initialBalance = BigDecimal.ZERO;
}

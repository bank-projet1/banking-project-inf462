package com.bankingproject.loanservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class LoanRequestDTO {

    @NotNull
    public Long customerId;

    @NotNull
    public Long accountId;

    @NotNull
    @Positive
    public Double amount;

    @NotNull
    @PositiveOrZero
    public Double interestRate;

    @NotNull
    @Positive
    public Integer durationMonths;

    public String ocrResult;
}

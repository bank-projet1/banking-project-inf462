package com.bankingproject.loanservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RepaymentDTO {

    @NotNull
    @Positive
    public Double amount;
}
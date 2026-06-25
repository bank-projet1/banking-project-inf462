package com.bankingproject.loanservice.dto;

import java.time.LocalDate;

public class LoanScheduleEntryDTO {

    private int installment;
    private LocalDate paymentDate;
    private Double paymentAmount;
    private Double principal;
    private Double interest;
    private Double remainingBalance;

    public LoanScheduleEntryDTO() {
    }

    public LoanScheduleEntryDTO(int installment, LocalDate paymentDate, Double paymentAmount, Double principal, Double interest, Double remainingBalance) {
        this.installment = installment;
        this.paymentDate = paymentDate;
        this.paymentAmount = paymentAmount;
        this.principal = principal;
        this.interest = interest;
        this.remainingBalance = remainingBalance;
    }

    public int getInstallment() {
        return installment;
    }

    public void setInstallment(int installment) {
        this.installment = installment;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Double getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public Double getPrincipal() {
        return principal;
    }

    public void setPrincipal(Double principal) {
        this.principal = principal;
    }

    public Double getInterest() {
        return interest;
    }

    public void setInterest(Double interest) {
        this.interest = interest;
    }

    public Double getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(Double remainingBalance) {
        this.remainingBalance = remainingBalance;
    }
}

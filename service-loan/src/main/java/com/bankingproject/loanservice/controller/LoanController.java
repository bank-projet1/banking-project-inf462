package com.bankingproject.loanservice.controller;

import com.bankingproject.loanservice.dto.LoanRequestDTO;
import com.bankingproject.loanservice.dto.LoanScheduleEntryDTO;
import com.bankingproject.loanservice.dto.RepaymentDTO;
import com.bankingproject.loanservice.model.Loan;
import com.bankingproject.loanservice.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @PostMapping
    public Loan create(@Valid @RequestBody LoanRequestDTO dto) {
        return service.createLoan(dto);
    }

    @GetMapping
    public java.util.List<Loan> getAll() {
        return service.getAllLoans();
    }

    @GetMapping("/{id}")
    public Loan get(@PathVariable Long id) {
        return service.getLoan(id);
    }

    @PutMapping("/{id}/review")
    public Loan review(@PathVariable Long id) {
        return service.review(id);
    }

    @PutMapping("/{id}/approve")
    public Loan approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PutMapping("/{id}/reject")
    public Loan reject(@PathVariable Long id) {
        return service.reject(id);
    }

    @PostMapping("/{id}/repay")
    public Loan repay(@PathVariable Long id, @Valid @RequestBody RepaymentDTO dto) {
        return service.repay(id, dto);
    }

    @GetMapping("/{id}/schedule")
    public java.util.List<LoanScheduleEntryDTO> schedule(@PathVariable Long id) {
        return service.schedule(id);
    }
}

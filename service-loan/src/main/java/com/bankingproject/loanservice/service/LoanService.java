
package com.bankingproject.loanservice.service;

import com.bankingproject.loanservice.client.AccountClient;
import com.bankingproject.loanservice.client.CustomerClient;
import com.bankingproject.loanservice.client.CustomerDTO;
import com.bankingproject.loanservice.client.DocumentIntelligenceClient;
import com.bankingproject.loanservice.client.NotificationClient;
import com.bankingproject.loanservice.client.NotificationRequest;
import com.bankingproject.loanservice.client.TransactionClient;
import com.bankingproject.loanservice.dto.DocumentAnalysisDTO;
import com.bankingproject.loanservice.dto.LoanRequestDTO;
import com.bankingproject.loanservice.dto.LoanScheduleEntryDTO;
import com.bankingproject.loanservice.dto.RepaymentDTO;
import com.bankingproject.loanservice.exception.BadRequestException;
import com.bankingproject.loanservice.exception.ExternalServiceException;
import com.bankingproject.loanservice.exception.ResourceNotFoundException;
import com.bankingproject.loanservice.model.Loan;
import com.bankingproject.loanservice.model.LoanStatus;
import com.bankingproject.loanservice.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LoanService {

    private final LoanRepository repository;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;
    private final NotificationClient notificationClient;
    private final TransactionClient transactionClient;
    private final DocumentIntelligenceClient documentIntelligenceClient;
    private final ObjectMapper objectMapper;

    public LoanService(LoanRepository repository,
                       AccountClient accountClient,
                       CustomerClient customerClient,
                       NotificationClient notificationClient,
                       TransactionClient transactionClient,
                       DocumentIntelligenceClient documentIntelligenceClient,
                       ObjectMapper objectMapper) {
        this.repository = repository;
        this.accountClient = accountClient;
        this.customerClient = customerClient;
        this.notificationClient = notificationClient;
        this.transactionClient = transactionClient;
        this.documentIntelligenceClient = documentIntelligenceClient;
        this.objectMapper = objectMapper;
    }

    public Loan createLoan(LoanRequestDTO dto) {
        validateLoanRequest(dto);
        validateCustomer(dto.customerId);
        validateAccount(dto.accountId);

        Loan loan = new Loan();
        loan.setCustomerId(dto.customerId);
        loan.setAccountId(dto.accountId);
        loan.setAmount(dto.amount);
        loan.setInterestRate(dto.interestRate);
        loan.setDurationMonths(dto.durationMonths);
        loan.setRemainingAmount(dto.amount);
        loan.setMonthlyPayment(calculateMonthlyPayment(dto.amount, dto.interestRate, dto.durationMonths));
        loan.setTotalPaid(0.0);
        loan.setStatus(LoanStatus.PENDING);
        loan.setCreatedAt(LocalDate.now());
        loan.setOcrResult(normalizeOcrResult(dto.ocrResult));
        Loan saved = repository.save(loan);
        notifyLoanEvent(saved, "REQUESTED", "Votre demande de pret de " + saved.getAmount() + " a ete enregistree.");
        return saved;
    }

    public Loan getLoan(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }

    public List<Loan> getAllLoans() {
        return repository.findAll();
    }

    public Loan review(Long id) {
        Loan loan = getLoan(id);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BadRequestException("Only PENDING loans can be moved to UNDER_REVIEW.");
        }
        requireDocumentAnalysis(loan);
        loan.setStatus(LoanStatus.UNDER_REVIEW);
        notifyLoanEvent(loan, "REVIEW_REQUESTED", "Loan is under review.");
        return repository.save(loan);
    }

    public Loan approve(Long id) {
        Loan loan = getLoan(id);
        if (loan.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new BadRequestException("Only UNDER_REVIEW loans can be approved.");
        }
        validateAiDecisionBeforeApproval(loan);
        transactionClient.deposit(loan.getAccountId(), BigDecimal.valueOf(loan.getAmount()));
        loan.setStatus(LoanStatus.APPROVED);
        loan.setStartDate(LocalDate.now());
        loan.setEndDate(loan.getStartDate().plusMonths(loan.getDurationMonths()));
        Loan saved = repository.save(loan);
        notifyLoanEvent(saved, "APPROVED",
                "Votre pret de " + saved.getAmount() + " a ete approuve et verse sur votre compte.");
        return saved;
    }

    public Loan reject(Long id) {
        Loan loan = getLoan(id);
        if (loan.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new BadRequestException("Only UNDER_REVIEW loans can be rejected.");
        }
        loan.setStatus(LoanStatus.REJECTED);
        Loan saved = repository.save(loan);
        notifyLoanEvent(saved, "REJECTED", "Votre demande de pret a ete rejetee.");
        return saved;
    }

    public Loan repay(Long id, RepaymentDTO dto) {
        Loan loan = getLoan(id);
        if (loan.getStatus() != LoanStatus.APPROVED && loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BadRequestException("Repayment is allowed only for APPROVED or ACTIVE loans.");
        }
        if (loan.getRemainingAmount() == null || loan.getRemainingAmount() <= 0) {
            throw new BadRequestException("This loan is already paid.");
        }
        if (dto.amount == null || dto.amount <= 0) {
            throw new BadRequestException("Repayment amount must be greater than 0.");
        }

        transactionClient.withdrawal(loan.getAccountId(), BigDecimal.valueOf(dto.amount));
        double remaining = loan.getRemainingAmount() - dto.amount;
        loan.setTotalPaid(round(loan.getTotalPaid() + dto.amount));
        loan.setRemainingAmount(Math.max(0.0, round(remaining)));
        if (loan.getRemainingAmount() <= 0) {
            loan.setStatus(LoanStatus.PAID);
        } else {
            loan.setStatus(LoanStatus.ACTIVE);
        }
        Loan saved = repository.save(loan);
        notifyLoanEvent(saved, "REPAYMENT", "Un remboursement a ete enregistre sur votre pret.");
        return saved;
    }

    public List<LoanScheduleEntryDTO> schedule(Long id) {
        Loan loan = getLoan(id);
        return buildSchedule(loan);
    }

    public Loan analyzeDocument(Long id, String documentType, MultipartFile file) {
        Loan loan = getLoan(id);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A loan document file is required.");
        }
        if (loan.getStatus() != LoanStatus.PENDING && loan.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new BadRequestException("Documents can be analyzed only before loan approval or rejection.");
        }

        String normalizedDocumentType = normalizeDocumentType(documentType);
        DocumentAnalysisDTO analysis = documentIntelligenceClient.analyze(normalizedDocumentType, file);
        if (analysis == null) {
            throw new ExternalServiceException("service-ai returned an empty analysis.", null);
        }

        loan.setDocumentType(analysis.documentType == null ? normalizedDocumentType : analysis.documentType);
        loan.setOcrScore(analysis.score);
        loan.setAiDecision(analysis.decision);
        loan.setDocumentAnalyzedAt(LocalDateTime.now());
        loan.setOcrResult(normalizeOcrResult(toJson(analysis)));

        Loan saved = repository.save(loan);
        notifyLoanEvent(saved, "DOCUMENT_ANALYZED",
                "Votre document de pret a ete analyse. Decision IA: " + saved.getAiDecision() + ".");
        return saved;
    }

    private void validateCustomer(Long customerId) {
        if (customerClient.getCustomerById(customerId) == null) {
            throw new BadRequestException("Customer not found: " + customerId);
        }
    }

    private void validateAccount(Long accountId) {
        if (accountClient.getAccountById(accountId) == null) {
            throw new BadRequestException("Account not found: " + accountId);
        }
    }

    private void notifyLoanEvent(Loan loan, String event, String message) {
        try {
            CustomerDTO customer = customerClient.getCustomerById(loan.getCustomerId());
            String fullMessage = message + " Pret #" + loan.getId() + ", compte #" + loan.getAccountId() + ".";
            notificationClient.sendNotification(new NotificationRequest(
                    loan.getCustomerId(),
                    "LOAN_" + event,
                    fullMessage,
                    "SUCCESS",
                    customer == null ? null : customer.getPhoneNumber()));
        } catch (ExternalServiceException ex) {
            // Notification failure should not block the core loan workflow.
            System.err.println("Notification service unavailable: " + ex.getMessage());
        }
    }

    private void validateLoanRequest(LoanRequestDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Loan request body is required.");
        }
        if (dto.customerId == null) {
            throw new BadRequestException("customerId is required.");
        }
        if (dto.accountId == null) {
            throw new BadRequestException("accountId is required.");
        }
        if (dto.amount == null || dto.amount <= 0) {
            throw new BadRequestException("amount must be greater than 0.");
        }
        if (dto.durationMonths == null || dto.durationMonths <= 0) {
            throw new BadRequestException("durationMonths must be greater than 0.");
        }
        if (dto.interestRate == null || dto.interestRate < 0) {
            throw new BadRequestException("interestRate cannot be negative.");
        }
    }

    private double calculateMonthlyPayment(Double amount, Double interestRate, Integer durationMonths) {
        if (interestRate == 0) {
            return round(amount / durationMonths);
        }
        double monthlyRate = interestRate / 100.0 / 12.0;
        double factor = Math.pow(1 + monthlyRate, durationMonths);
        return round(amount * monthlyRate * factor / (factor - 1));
    }

    private String normalizeOcrResult(String ocrResult) {
        if (ocrResult == null || ocrResult.isBlank()) {
            return null;
        }
        return ocrResult.length() <= 5000 ? ocrResult : ocrResult.substring(0, 5000);
    }

    private String normalizeDocumentType(String documentType) {
        return documentType == null || documentType.isBlank() ? "LOAN_APPLICATION" : documentType.trim().toUpperCase();
    }

    private String toJson(DocumentAnalysisDTO analysis) {
        try {
            return objectMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Unable to serialize document analysis.", ex);
        }
    }

    private void requireDocumentAnalysis(Loan loan) {
        if (loan.getOcrResult() == null || loan.getOcrResult().isBlank()) {
            throw new BadRequestException("A document OCR analysis is required before reviewing the loan file.");
        }
    }

    private void validateAiDecisionBeforeApproval(Loan loan) {
        requireDocumentAnalysis(loan);
        if ("REJECTED".equalsIgnoreCase(loan.getAiDecision())) {
            throw new BadRequestException("This loan file was rejected by document analysis and cannot be approved.");
        }
    }

    private List<LoanScheduleEntryDTO> buildSchedule(Loan loan) {
        List<LoanScheduleEntryDTO> schedule = new ArrayList<>();
        double balance = loan.getAmount();
        double monthlyRate = loan.getInterestRate() / 100.0 / 12.0;
        LocalDate paymentDate = loan.getStartDate() != null ? loan.getStartDate() : LocalDate.now();
        double payment = loan.getMonthlyPayment();

        for (int installment = 1; installment <= loan.getDurationMonths(); installment++) {
            double interest = round(balance * monthlyRate);
            double principal = round(payment - interest);
            if (installment == loan.getDurationMonths()) {
                principal = round(balance);
                payment = round(principal + interest);
            }
            balance = round(balance - principal);
            schedule.add(new LoanScheduleEntryDTO(
                    installment,
                    paymentDate.plusMonths(installment - 1),
                    payment,
                    principal,
                    interest,
                    Math.max(0.0, balance)));
        }
        return schedule;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

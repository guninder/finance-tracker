package com.financetracker.server.controller;

import com.financetracker.server.dto.SummaryResponse;
import com.financetracker.server.dto.TransactionRequest;
import com.financetracker.server.dto.TransactionResponse;
import com.financetracker.server.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(transactionService.getTransactions(email));
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            Authentication authentication,
            @Valid @RequestBody TransactionRequest request) {
        String email = authentication.getName();
        return ResponseEntity.ok(transactionService.createTransaction(email, request));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        String email = authentication.getName();
        return ResponseEntity.ok(transactionService.updateTransaction(email, id, request));
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(
            Authentication authentication,
            @PathVariable Long id) {
        String email = authentication.getName();
        transactionService.deleteTransaction(email, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(transactionService.getSummary(email));
    }
}

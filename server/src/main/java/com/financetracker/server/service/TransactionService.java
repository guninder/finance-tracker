package com.financetracker.server.service;

import com.financetracker.server.dto.SummaryResponse;
import com.financetracker.server.dto.TransactionRequest;
import com.financetracker.server.dto.TransactionResponse;
import com.financetracker.server.model.Transaction;
import com.financetracker.server.model.TransactionType;
import com.financetracker.server.model.User;
import com.financetracker.server.repository.TransactionRepository;
import com.financetracker.server.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public List<TransactionResponse> getTransactions(String email) {
        User user = getUserByEmail(email);
        return transactionRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TransactionResponse createTransaction(String email, TransactionRequest request) {
        User user = getUserByEmail(email);
        TransactionType type = TransactionType.valueOf(request.getType().toUpperCase());

        Transaction transaction = Transaction.builder()
                .type(type)
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .date(request.getDate())
                .user(user)
                .build();

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    public TransactionResponse updateTransaction(String email, Long id, TransactionRequest request) {
        User user = getUserByEmail(email);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Transaction not found");
        }

        TransactionType type = TransactionType.valueOf(request.getType().toUpperCase());
        transaction.setType(type);
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transaction.setDate(request.getDate());

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    public void deleteTransaction(String email, Long id) {
        User user = getUserByEmail(email);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Transaction not found");
        }

        transactionRepository.delete(transaction);
    }

    public SummaryResponse getSummary(String email) {
        User user = getUserByEmail(email);
        BigDecimal totalIncome = transactionRepository.sumByUserIdAndType(user.getId(), TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository.sumByUserIdAndType(user.getId(), TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpenses);

        return new SummaryResponse(totalIncome, totalExpenses, balance);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .date(transaction.getDate())
                .build();
    }
}

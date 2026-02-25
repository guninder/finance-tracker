package com.financetracker.server.service;

import com.financetracker.server.dto.SummaryResponse;
import com.financetracker.server.dto.TransactionRequest;
import com.financetracker.server.dto.TransactionResponse;
import com.financetracker.server.model.Transaction;
import com.financetracker.server.model.TransactionType;
import com.financetracker.server.model.User;
import com.financetracker.server.repository.TransactionRepository;
import com.financetracker.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("encoded-password")
                .build();

        testTransaction = Transaction.builder()
                .id(1L)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("50.00"))
                .category("Food")
                .description("Lunch")
                .date(LocalDate.of(2026, 2, 25))
                .user(testUser)
                .build();
    }

    @Test
    void getTransactions_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserIdOrderByDateDesc(1L)).thenReturn(List.of(testTransaction));

        List<TransactionResponse> result = transactionService.getTransactions("test@example.com");

        assertEquals(1, result.size());
        assertEquals("EXPENSE", result.get(0).getType());
        assertEquals(new BigDecimal("50.00"), result.get(0).getAmount());
        assertEquals("Food", result.get(0).getCategory());
        assertEquals("Lunch", result.get(0).getDescription());
    }

    @Test
    void createTransaction_success() {
        TransactionRequest request = new TransactionRequest(
                "EXPENSE", new BigDecimal("50.00"), "Food", "Lunch", LocalDate.of(2026, 2, 25));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        TransactionResponse result = transactionService.createTransaction("test@example.com", request);

        assertNotNull(result);
        assertEquals("EXPENSE", result.getType());
        assertEquals(new BigDecimal("50.00"), result.getAmount());
        assertEquals("Food", result.getCategory());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_success() {
        TransactionRequest request = new TransactionRequest(
                "INCOME", new BigDecimal("100.00"), "Salary", "Monthly pay", LocalDate.of(2026, 2, 25));

        Transaction updatedTransaction = Transaction.builder()
                .id(1L)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("100.00"))
                .category("Salary")
                .description("Monthly pay")
                .date(LocalDate.of(2026, 2, 25))
                .user(testUser)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(updatedTransaction);

        TransactionResponse result = transactionService.updateTransaction("test@example.com", 1L, request);

        assertNotNull(result);
        assertEquals("INCOME", result.getType());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals("Salary", result.getCategory());
    }

    @Test
    void updateTransaction_notOwned_throwsException() {
        User otherUser = User.builder().id(2L).name("Other").email("other@example.com").build();
        Transaction otherTransaction = Transaction.builder()
                .id(2L)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("30.00"))
                .category("Food")
                .user(otherUser)
                .build();

        TransactionRequest request = new TransactionRequest(
                "EXPENSE", new BigDecimal("30.00"), "Food", "Dinner", LocalDate.of(2026, 2, 25));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(otherTransaction));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.updateTransaction("test@example.com", 2L, request)
        );

        assertEquals("Transaction not found", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteTransaction_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        assertDoesNotThrow(() -> transactionService.deleteTransaction("test@example.com", 1L));

        verify(transactionRepository).delete(testTransaction);
    }

    @Test
    void deleteTransaction_notFound_throwsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.deleteTransaction("test@example.com", 99L)
        );

        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void getSummary_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.sumByUserIdAndType(1L, TransactionType.INCOME))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumByUserIdAndType(1L, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("2000.00"));

        SummaryResponse result = transactionService.getSummary("test@example.com");

        assertNotNull(result);
        assertEquals(new BigDecimal("5000.00"), result.getTotalIncome());
        assertEquals(new BigDecimal("2000.00"), result.getTotalExpenses());
        assertEquals(new BigDecimal("3000.00"), result.getBalance());
    }
}

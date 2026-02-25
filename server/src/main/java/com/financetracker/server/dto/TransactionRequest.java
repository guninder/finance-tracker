package com.financetracker.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotNull(message = "Type is required")
    private String type;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must be at most 100 characters")
    private String category;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @NotNull(message = "Date is required")
    private LocalDate date;
}

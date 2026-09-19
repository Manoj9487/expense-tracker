
package com.expensetracker.expense_tracker.service;

import com.expensetracker.expense_tracker.dto.ExpenseRequestDTO;
import com.expensetracker.expense_tracker.dto.ExpenseResponseDTO;
import com.expensetracker.expense_tracker.entity.Category;
import com.expensetracker.expense_tracker.entity.Expense;
import com.expensetracker.expense_tracker.entity.PaymentMethod;
import com.expensetracker.expense_tracker.exception.ExpenseNotFoundException;
import com.expensetracker.expense_tracker.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createExpense_savesAndReturnsMappedResponse() {
        // Arrange
        ExpenseRequestDTO requestDto = new ExpenseRequestDTO();
        requestDto.setTitle("Groceries");
        requestDto.setAmount(new BigDecimal("45.50"));
        requestDto.setCategory(Category.FOOD);
        requestDto.setExpenseDate(LocalDate.of(2026, 9, 10));
        requestDto.setPaymentMethod(PaymentMethod.UPI);
        requestDto.setDescription("Weekly shop");

        Expense savedEntity = new Expense();
        savedEntity.setId(1L);
        savedEntity.setTitle("Groceries");
        savedEntity.setAmount(new BigDecimal("45.50"));
        savedEntity.setCategory(Category.FOOD);
        savedEntity.setExpenseDate(LocalDate.of(2026, 9, 10));
        savedEntity.setPaymentMethod(PaymentMethod.UPI);
        savedEntity.setDescription("Weekly shop");
        savedEntity.setCreatedAt(LocalDateTime.now());

        when(expenseRepository.save(any(Expense.class))).thenReturn(savedEntity);

        // Act
        ExpenseResponseDTO result = expenseService.createExpense(requestDto);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Groceries");
        assertThat(result.getAmount()).isEqualByComparingTo("45.50");
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    void getExpenseById_whenNotFound_throwsExpenseNotFoundException() {
        // Arrange
        when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> expenseService.getExpenseById(999L))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void getExpenseById_whenFound_returnsMappedResponse() {
        // Arrange
        Expense expense = new Expense();
        expense.setId(5L);
        expense.setTitle("Bus pass");
        expense.setAmount(new BigDecimal("20.00"));
        expense.setCategory(Category.TRANSPORT);
        expense.setExpenseDate(LocalDate.of(2026, 9, 12));
        expense.setPaymentMethod(PaymentMethod.CASH);
        expense.setCreatedAt(LocalDateTime.now());

        when(expenseRepository.findById(5L)).thenReturn(Optional.of(expense));

        // Act
        ExpenseResponseDTO result = expenseService.getExpenseById(5L);

        // Assert
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getCategory()).isEqualTo(Category.TRANSPORT);
    }

    @Test
    void deleteExpense_whenNotFound_throwsAndNeverCallsDelete() {
        // Arrange
        when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> expenseService.deleteExpense(999L))
                .isInstanceOf(ExpenseNotFoundException.class);

        // This is the important extra check: confirm delete() was NEVER
        // called on the repository, since the entity was never found.
        verify(expenseRepository, never()).delete(any(Expense.class));
    }

    @Test
    void updateExpense_preservesOriginalCreatedAt() {
        // Arrange
        LocalDateTime originalCreatedAt = LocalDateTime.of(2026, 9, 1, 10, 0);

        Expense existing = new Expense();
        existing.setId(3L);
        existing.setTitle("Old title");
        existing.setAmount(new BigDecimal("10.00"));
        existing.setCategory(Category.OTHER);
        existing.setExpenseDate(LocalDate.of(2026, 9, 1));
        existing.setPaymentMethod(PaymentMethod.CASH);
        existing.setCreatedAt(originalCreatedAt);

        when(expenseRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseRequestDTO updateDto = new ExpenseRequestDTO();
        updateDto.setTitle("New title");
        updateDto.setAmount(new BigDecimal("99.00"));
        updateDto.setCategory(Category.SHOPPING);
        updateDto.setExpenseDate(LocalDate.of(2026, 9, 2));
        updateDto.setPaymentMethod(PaymentMethod.CARD);

        // Act
        ExpenseResponseDTO result = expenseService.updateExpense(3L, updateDto);

        // Assert
        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getCreatedAt()).isEqualTo(originalCreatedAt); // unchanged!
    }
}
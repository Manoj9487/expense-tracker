package com.expensetracker.expense_tracker.controller;

import com.expensetracker.expense_tracker.dto.CategorySummaryDTO;
import com.expensetracker.expense_tracker.dto.ExpenseRequestDTO;
import com.expensetracker.expense_tracker.dto.ExpenseResponseDTO;
import com.expensetracker.expense_tracker.entity.Category;
import com.expensetracker.expense_tracker.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    // ===== Create =====

    @PostMapping
    public ResponseEntity<ExpenseResponseDTO> createExpense(@Valid @RequestBody ExpenseRequestDTO dto) {
        ExpenseResponseDTO created = expenseService.createExpense(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ===== Read =====

    @GetMapping
    public ResponseEntity<List<ExpenseResponseDTO>> getAllExpenses(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String direction) {
        List<ExpenseResponseDTO> expenses = expenseService.getAllExpenses(sortBy, direction);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> getExpenseById(@PathVariable Long id) {
        ExpenseResponseDTO expense = expenseService.getExpenseById(id);
        return ResponseEntity.ok(expense);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ExpenseResponseDTO>> getExpensesByCategory(@PathVariable Category category) {
        List<ExpenseResponseDTO> expenses = expenseService.getExpensesByCategory(category);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<ExpenseResponseDTO>> getExpensesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ExpenseResponseDTO> expenses = expenseService.getExpensesByDateRange(startDate, endDate);
        return ResponseEntity.ok(expenses);
    }

    // ===== Update =====

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequestDTO dto) {
        ExpenseResponseDTO updated = expenseService.updateExpense(id, dto);
        return ResponseEntity.ok(updated);
    }

    // ===== Delete =====

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Summaries =====

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        BigDecimal total = expenseService.getTotalExpenses();
        List<CategorySummaryDTO> categoryBreakdown = expenseService.getCategoryWiseSpending();

        Map<String, Object> summary = Map.of(
                "totalExpenses", total,
                "categoryBreakdown", categoryBreakdown
        );
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<Map<String, Object>> getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month) {
        BigDecimal monthlyTotal = expenseService.getMonthlyTotal(year, month);

        Map<String, Object> summary = Map.of(
                "year", year,
                "month", month,
                "totalExpenses", monthlyTotal
        );
        return ResponseEntity.ok(summary);
    }
}
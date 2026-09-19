package com.expensetracker.expense_tracker.service;

import com.expensetracker.expense_tracker.dto.CategorySummaryDTO;
import com.expensetracker.expense_tracker.dto.ExpenseRequestDTO;
import com.expensetracker.expense_tracker.dto.ExpenseResponseDTO;
import com.expensetracker.expense_tracker.entity.Category;
import com.expensetracker.expense_tracker.entity.Expense;
import com.expensetracker.expense_tracker.exception.ExpenseNotFoundException;
import com.expensetracker.expense_tracker.repository.ExpenseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public ExpenseResponseDTO createExpense(ExpenseRequestDTO dto) {
        Expense expense = mapToEntity(dto);
        Expense saved = expenseRepository.save(expense);
        return mapToResponseDTO(saved);
    }

    public List<ExpenseResponseDTO> getAllExpenses(String sortBy, String direction) {
        Sort sort = buildSort(sortBy, direction);
        return expenseRepository.findAll(sort)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public ExpenseResponseDTO getExpenseById(Long id) {
        Expense expense = findEntityOrThrow(id);
        return mapToResponseDTO(expense);
    }

    public List<ExpenseResponseDTO> getExpensesByCategory(Category category) {
        return expenseRepository.findByCategory(category)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ExpenseResponseDTO> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByExpenseDateBetween(startDate, endDate)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }


    public ExpenseResponseDTO updateExpense(Long id, ExpenseRequestDTO dto) {
        Expense existing = findEntityOrThrow(id);

        existing.setTitle(dto.getTitle());
        existing.setAmount(dto.getAmount());
        existing.setCategory(dto.getCategory());
        existing.setExpenseDate(dto.getExpenseDate());
        existing.setPaymentMethod(dto.getPaymentMethod());
        existing.setDescription(dto.getDescription());


        Expense updated = expenseRepository.save(existing);
        return mapToResponseDTO(updated);
    }


    public void deleteExpense(Long id) {
        Expense existing = findEntityOrThrow(id);
        expenseRepository.delete(existing);
    }

    public BigDecimal getTotalExpenses() {
        return expenseRepository.getTotalExpenses();
    }

    public BigDecimal getMonthlyTotal(int year, int month) {
        return expenseRepository.getTotalExpensesForMonth(year, month);
    }

    public List<CategorySummaryDTO> getCategoryWiseSpending() {
        List<Object[]> rawResults = expenseRepository.getCategoryWiseSpending();
        return rawResults.stream()
                .map(row -> new CategorySummaryDTO((Category) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());
    }


    private Expense findEntityOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    private Sort buildSort(String sortBy, String direction) {
        String property = (sortBy == null || sortBy.isBlank()) ? "expenseDate" : sortBy;
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, property);
    }

    private Expense mapToEntity(ExpenseRequestDTO dto) {
        Expense expense = new Expense();
        expense.setTitle(dto.getTitle());
        expense.setAmount(dto.getAmount());
        expense.setCategory(dto.getCategory());
        expense.setExpenseDate(dto.getExpenseDate());
        expense.setPaymentMethod(dto.getPaymentMethod());
        expense.setDescription(dto.getDescription());
        return expense;
    }

    private ExpenseResponseDTO mapToResponseDTO(Expense expense) {
        return new ExpenseResponseDTO(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getExpenseDate(),
                expense.getPaymentMethod(),
                expense.getDescription(),
                expense.getCreatedAt()
        );
    }
}
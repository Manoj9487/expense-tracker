package com.expensetracker.expense_tracker.repository;

import com.expensetracker.expense_tracker.entity.Category;
import com.expensetracker.expense_tracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByCategory(Category category);

    List<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate);
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
    BigDecimal getTotalExpenses();

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month")
    BigDecimal getTotalExpensesForMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) FROM Expense e GROUP BY e.category")
    List<Object[]> getCategoryWiseSpending();

}

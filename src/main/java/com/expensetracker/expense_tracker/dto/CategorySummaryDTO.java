package com.expensetracker.expense_tracker.dto;

import com.expensetracker.expense_tracker.entity.Category;
import java.math.BigDecimal;

public class CategorySummaryDTO {

    private Category category;
    private BigDecimal totalAmount;

    public CategorySummaryDTO() {
    }

    public CategorySummaryDTO(Category category, BigDecimal totalAmount) {
        this.category = category;
        this.totalAmount = totalAmount;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
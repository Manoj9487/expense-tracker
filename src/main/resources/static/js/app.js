const API_BASE_URL = "http://localhost:8081/api/expenses";


const expenseForm = document.getElementById("expenseForm");
const expenseIdInput = document.getElementById("expenseId");
const titleInput = document.getElementById("title");
const amountInput = document.getElementById("amount");
const categoryInput = document.getElementById("category");
const expenseDateInput = document.getElementById("expenseDate");
const paymentMethodInput = document.getElementById("paymentMethod");
const descriptionInput = document.getElementById("description");
const formError = document.getElementById("formError");
const formTitle = document.getElementById("formTitle");
const submitBtn = document.getElementById("submitBtn");
const cancelEditBtn = document.getElementById("cancelEditBtn");

const expenseListEl = document.getElementById("expenseList");
const emptyState = document.getElementById("emptyState");

const totalSpendingEl = document.getElementById("totalSpending");
const monthlySpendingEl = document.getElementById("monthlySpending");
const categoryListEl = document.getElementById("categoryList");

const filterCategory = document.getElementById("filterCategory");
const filterStartDate = document.getElementById("filterStartDate");
const filterEndDate = document.getElementById("filterEndDate");
const sortBySelect = document.getElementById("sortBy");
const sortDirectionSelect = document.getElementById("sortDirection");
const applyFiltersBtn = document.getElementById("applyFiltersBtn");
const clearFiltersBtn = document.getElementById("clearFiltersBtn");


document.addEventListener("DOMContentLoaded", () => {
    loadExpenses();
    loadSummary();

    expenseForm.addEventListener("submit", handleFormSubmit);
    cancelEditBtn.addEventListener("click", exitEditMode);
    applyFiltersBtn.addEventListener("click", loadExpenses);
    clearFiltersBtn.addEventListener("click", clearFilters);
});

async function loadExpenses() {
    if (!validateDateRange()) return;

    showListLoading();
    updateActiveFilterIndicator();

    try {
        const url = buildExpenseListUrl();
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error("Failed to load expenses");
        }

        let expenses = await response.json();
        expenses = applyClientSideSort(expenses);
        renderExpenseList(expenses);
    } catch (error) {
        console.error(error);
        expenseListEl.innerHTML = `<p class="empty-state">Something went wrong loading expenses.</p>`;
    }
}

function updateActiveFilterIndicator() {
    const hasCategory = filterCategory.value !== "";
    const hasDateRange = filterStartDate.value !== "" && filterEndDate.value !== "";

    clearFiltersBtn.textContent = (hasCategory || hasDateRange)
        ? "Clear Filters (active)"
        : "Clear";
}

function buildExpenseListUrl() {

    const category = filterCategory.value;
    const startDate = filterStartDate.value;
    const endDate = filterEndDate.value;
    const sortBy = sortBySelect.value;
    const direction = sortDirectionSelect.value;

    if (category) {
        return `${API_BASE_URL}/category/${category}`;
    }

    if (startDate && endDate) {
        return `${API_BASE_URL}/date-range?startDate=${startDate}&endDate=${endDate}`;
    }

    return `${API_BASE_URL}?sortBy=${sortBy}&direction=${direction}`;
}

function applyClientSideSort(expenses) {
    const sortBy = sortBySelect.value;   // "expenseDate" or "amount"
    const direction = sortDirectionSelect.value; // "asc" or "desc"

    const sorted = [...expenses].sort((a, b) => {
        let valA = a[sortBy];
        let valB = b[sortBy];

        if (sortBy === "amount") {
            valA = Number(valA);
            valB = Number(valB);
        }
        // expenseDate strings ("yyyy-MM-dd") sort correctly as plain strings

        if (valA < valB) return direction === "asc" ? -1 : 1;
        if (valA > valB) return direction === "asc" ? 1 : -1;
        return 0;
    });

    return sorted;
}

function validateDateRange() {
    const startDate = filterStartDate.value;
    const endDate = filterEndDate.value;

    if (startDate && endDate && startDate > endDate) {
        expenseListEl.innerHTML =
            `<p class="empty-state">Start date must be before end date.</p>`;
        return false;
    }
    return true;
}

function showListLoading() {
    expenseListEl.innerHTML = `<p class="empty-state">Loading...</p>`;
}

function renderExpenseList(expenses) {
    if (!expenses || expenses.length === 0) {
        expenseListEl.innerHTML = `<p class="empty-state">No expenses yet. Add one above.</p>`;
        return;
    }

    expenseListEl.innerHTML = expenses.map(expense => `
        <div class="expense-item">
            <div class="expense-main">
                <span class="expense-title">${escapeHtml(expense.title)}</span>
                <span class="expense-meta">
                    ${formatCategory(expense.category)} &middot;
                    ${formatDate(expense.expenseDate)} &middot;
                    ${formatPaymentMethod(expense.paymentMethod)}
                </span>
            </div>
            <div>
                <span class="expense-amount">${formatCurrency(expense.amount)}</span>
                <span class="expense-actions">
                    <button type="button" onclick="startEdit(${expense.id})">Edit</button>
                    <button type="button" class="delete-btn" onclick="deleteExpense(${expense.id})">Delete</button>
                </span>
            </div>
        </div>
    `).join("");
}
async function loadSummary() {
    totalSpendingEl.textContent = "...";
    monthlySpendingEl.textContent = "...";

    try {
        const response = await fetch(`${API_BASE_URL}/summary`);
        if (!response.ok) throw new Error("Failed to load summary");

        const summary = await response.json();
        totalSpendingEl.textContent = formatCurrency(summary.totalExpenses);
        renderCategoryBreakdown(summary.categoryBreakdown);

        const now = new Date();
        const monthResponse = await fetch(
            `${API_BASE_URL}/monthly-summary?year=${now.getFullYear()}&month=${now.getMonth() + 1}`
        );
        if (!monthResponse.ok) throw new Error("Failed to load monthly summary");

        const monthSummary = await monthResponse.json();
        monthlySpendingEl.textContent = formatCurrency(monthSummary.totalExpenses);
    } catch (error) {
        console.error(error);
        totalSpendingEl.textContent = "—";
        monthlySpendingEl.textContent = "—";
    }
}


function renderCategoryBreakdown(categoryBreakdown) {
    if (!categoryBreakdown || categoryBreakdown.length === 0) {
        categoryListEl.innerHTML = `<div>No data yet</div>`;
        return;
    }

    categoryListEl.innerHTML = categoryBreakdown.map(entry => `
        <div>
            <span>${formatCategory(entry.category)}</span>
            <span>${formatCurrency(entry.totalAmount)}</span>
        </div>
    `).join("");
}

async function handleFormSubmit(event) {
    event.preventDefault();
    formError.textContent = "";

    const payload = {
        title: titleInput.value.trim(),
        amount: parseFloat(amountInput.value),
        category: categoryInput.value,
        expenseDate: expenseDateInput.value,
        paymentMethod: paymentMethodInput.value,
        description: descriptionInput.value.trim()
    };

    const existingId = expenseIdInput.value;
    const isEditing = existingId !== "";

    const url = isEditing ? `${API_BASE_URL}/${existingId}` : API_BASE_URL;
    const method = isEditing ? "PUT" : "POST";

    try {
        const response = await fetch(url, {
            method: method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorBody = await response.json();
            showFormErrors(errorBody);
            return;
        }

        resetForm();
        exitEditMode();
        loadExpenses();
        loadSummary();
    } catch (error) {
        console.error(error);
        formError.textContent = "Network error. Please check your connection and try again.";
    }
}

function showFormErrors(errorBody) {
    if (errorBody.fieldErrors) {
        const messages = Object.entries(errorBody.fieldErrors)
            .map(([field, message]) => `${field}: ${message}`)
            .join(" | ");
        formError.textContent = messages;
    } else {
        formError.textContent = errorBody.message || "Something went wrong. Please try again.";
    }
}

async function startEdit(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/${id}`);
        if (!response.ok) throw new Error("Failed to load expense");

        const expense = await response.json();

        expenseIdInput.value = expense.id;
        titleInput.value = expense.title;
        amountInput.value = expense.amount;
        categoryInput.value = expense.category;
        expenseDateInput.value = expense.expenseDate;
        paymentMethodInput.value = expense.paymentMethod;
        descriptionInput.value = expense.description || "";

        formTitle.textContent = "Edit Expense";
        submitBtn.textContent = "Update Expense";
        cancelEditBtn.classList.remove("hidden");

        expenseForm.scrollIntoView({ behavior: "smooth" });
    } catch (error) {
        console.error(error);
        formError.textContent = "Could not load this expense for editing.";
    }
}

function exitEditMode() {
    resetForm();
    formTitle.textContent = "Add Expense";
    submitBtn.textContent = "Add Expense";
    cancelEditBtn.classList.add("hidden");
}

function resetForm() {
    expenseForm.reset();
    expenseIdInput.value = "";
    formError.textContent = "";
}


async function deleteExpense(id) {
    const confirmed = confirm("Delete this expense? This cannot be undone.");
    if (!confirmed) return;

    try {
        const response = await fetch(`${API_BASE_URL}/${id}`, { method: "DELETE" });

        if (!response.ok && response.status !== 204) {
            throw new Error("Failed to delete expense");
        }

        loadExpenses();
        loadSummary();
    } catch (error) {
        console.error(error);
        alert("Could not delete this expense. Please try again.");
    }
}

function clearFilters() {
    filterCategory.value = "";
    filterStartDate.value = "";
    filterEndDate.value = "";
    sortBySelect.value = "expenseDate";
    sortDirectionSelect.value = "desc";
    clearFiltersBtn.textContent = "Clear";
    loadExpenses();
}

function formatCurrency(amount) {
    return `₹${Number(amount).toFixed(2)}`;
}

function formatDate(dateStr) {
    const [year, month, day] = dateStr.split("-");
    return `${month}/${day}/${year}`;
}

function formatCategory(category) {
    return category.charAt(0) + category.slice(1).toLowerCase();
}

function formatPaymentMethod(method) {
    return method.replace("_", " ").toLowerCase()
        .replace(/\b\w/g, char => char.toUpperCase());
}

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str;
    return div.innerHTML;
}
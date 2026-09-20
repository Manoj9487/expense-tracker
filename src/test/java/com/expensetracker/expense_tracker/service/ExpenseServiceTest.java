package com.expensetracker.expense_tracker.service;

import com.expensetracker.expense_tracker.dto.ExpenseRequestDTO;
import com.expensetracker.expense_tracker.dto.ExpenseResponseDTO;
import com.expensetracker.expense_tracker.entity.Category;
import com.expensetracker.expense_tracker.entity.Expense;
import com.expensetracker.expense_tracker.entity.PaymentMethod;
import com.expensetracker.expense_tracker.entity.User;
import com.expensetracker.expense_tracker.exception.ExpenseNotFoundException;
import com.expensetracker.expense_tracker.repository.ExpenseRepository;
import com.expensetracker.expense_tracker.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // ===== Simulate a logged-in user for every test =====
        // ExpenseService reads the username from SecurityContextHolder and
        // looks it up via UserRepository, so both need to be stubbed here.
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("manoj");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("manoj");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("manoj")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        // Prevent this fake logged-in user from leaking into other test classes
        SecurityContextHolder.clearContext();
    }

    @Test
    void createExpense_setsCurrentUserAndReturnsMappedResponse() {
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
        savedEntity.setUser(currentUser);
        savedEntity.setCreatedAt(LocalDateTime.now());

        when(expenseRepository.save(any(Expense.class))).thenReturn(savedEntity);

        // Act
        ExpenseResponseDTO result = expenseService.createExpense(requestDto);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Groceries");
        assertThat(result.getAmount()).isEqualByComparingTo("45.50");

        // Capture what was actually passed to save() to confirm the
        // logged-in user was attached before persisting — this is the
        // exact bug we hit in production when this check was missing.
        var captor = org.mockito.ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(currentUser);
    }

    @Test
    void getExpenseById_whenNotFoundForCurrentUser_throwsExpenseNotFoundException() {
        // Arrange
        when(expenseRepository.findByIdAndUser(999L, currentUser)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> expenseService.getExpenseById(999L))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void getExpenseById_whenFoundForCurrentUser_returnsMappedResponse() {
        // Arrange
        Expense expense = new Expense();
        expense.setId(5L);
        expense.setTitle("Bus pass");
        expense.setAmount(new BigDecimal("20.00"));
        expense.setCategory(Category.TRANSPORT);
        expense.setExpenseDate(LocalDate.of(2026, 9, 12));
        expense.setPaymentMethod(PaymentMethod.CASH);
        expense.setUser(currentUser);
        expense.setCreatedAt(LocalDateTime.now());

        when(expenseRepository.findByIdAndUser(5L, currentUser)).thenReturn(Optional.of(expense));

        // Act
        ExpenseResponseDTO result = expenseService.getExpenseById(5L);

        // Assert
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getCategory()).isEqualTo(Category.TRANSPORT);
    }

    @Test
    void getAllExpenses_onlyReturnsExpensesForCurrentUser() {
        // Arrange
        Expense expense = new Expense();
        expense.setId(7L);
        expense.setTitle("Netflix");
        expense.setAmount(new BigDecimal("199.00"));
        expense.setCategory(Category.ENTERTAINMENT);
        expense.setExpenseDate(LocalDate.of(2026, 9, 15));
        expense.setPaymentMethod(PaymentMethod.CARD);
        expense.setUser(currentUser);
        expense.setCreatedAt(LocalDateTime.now());

        when(expenseRepository.findByUser(eq(currentUser), any())).thenReturn(List.of(expense));

        // Act
        List<ExpenseResponseDTO> result = expenseService.getAllExpenses("expenseDate", "desc");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Netflix");
        // Confirms the query is scoped to the logged-in user, not a global findAll()
        verify(expenseRepository, times(1)).findByUser(eq(currentUser), any());
        verify(expenseRepository, never()).findAll();
    }

    @Test
    void deleteExpense_whenNotFoundForCurrentUser_throwsAndNeverCallsDelete() {
        // Arrange
        when(expenseRepository.findByIdAndUser(999L, currentUser)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> expenseService.deleteExpense(999L))
                .isInstanceOf(ExpenseNotFoundException.class);

        // Confirm delete() was NEVER called on the repository, since the entity was never found
        verify(expenseRepository, never()).delete(any(Expense.class));
    }

    @Test
    void deleteExpense_whenFoundForCurrentUser_deletesIt() {
        // Arrange
        Expense expense = new Expense();
        expense.setId(8L);
        expense.setUser(currentUser);
        when(expenseRepository.findByIdAndUser(8L, currentUser)).thenReturn(Optional.of(expense));

        // Act
        expenseService.deleteExpense(8L);

        // Assert
        verify(expenseRepository, times(1)).delete(expense);
    }

    @Test
    void updateExpense_preservesOriginalCreatedAtAndOwner() {
        // Arrange
        LocalDateTime originalCreatedAt = LocalDateTime.of(2026, 9, 1, 10, 0);

        Expense existing = new Expense();
        existing.setId(3L);
        existing.setTitle("Old title");
        existing.setAmount(new BigDecimal("10.00"));
        existing.setCategory(Category.OTHER);
        existing.setExpenseDate(LocalDate.of(2026, 9, 1));
        existing.setPaymentMethod(PaymentMethod.CASH);
        existing.setUser(currentUser);
        existing.setCreatedAt(originalCreatedAt);

        when(expenseRepository.findByIdAndUser(3L, currentUser)).thenReturn(Optional.of(existing));
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
        assertThat(existing.getUser()).isEqualTo(currentUser); // ownership untouched by an update
    }

    @Test
    void updateExpense_whenBelongsToAnotherUser_throwsExpenseNotFoundException() {
        // Arrange — repository is stubbed with findByIdAndUser, which returns
        // empty when the expense exists but belongs to someone else. This is
        // the test that proves users can't edit each other's data.
        when(expenseRepository.findByIdAndUser(4L, currentUser)).thenReturn(Optional.empty());

        ExpenseRequestDTO updateDto = new ExpenseRequestDTO();
        updateDto.setTitle("Attempted takeover");
        updateDto.setAmount(new BigDecimal("1.00"));
        updateDto.setCategory(Category.OTHER);
        updateDto.setExpenseDate(LocalDate.now());
        updateDto.setPaymentMethod(PaymentMethod.CASH);

        // Act + Assert
        assertThatThrownBy(() -> expenseService.updateExpense(4L, updateDto))
                .isInstanceOf(ExpenseNotFoundException.class);

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void getTotalExpenses_isScopedToCurrentUser() {
        // Arrange
        when(expenseRepository.getTotalExpenses(currentUser)).thenReturn(new BigDecimal("350.00"));

        // Act
        BigDecimal result = expenseService.getTotalExpenses();

        // Assert
        assertThat(result).isEqualByComparingTo("350.00");
        verify(expenseRepository, times(1)).getTotalExpenses(currentUser);
    }
}
# Personal Expense Tracker

A full-stack CRUD application for tracking personal expenses, built with Java, Spring Boot, MySQL, and vanilla JavaScript.

## Features

- Add, view, update, and delete expenses
- Filter by category or date range
- Sort by date or amount
- Dashboard with total spending, monthly spending, and category-wise breakdown
- Full backend validation with clear, field-level error messages
- Automated unit and integration tests

## Tech Stack

**Backend:** Java 21, Spring Boot 4, Spring Data JPA, Spring MVC, Bean Validation, Maven
**Database:** MySQL (H2 for automated tests)
**Frontend:** HTML, CSS, vanilla JavaScript (fetch API)
**Testing:** JUnit 5, Mockito, AssertJ, MockMvc

## Architecture

```
Controller → Service → Repository → Database
```
- **Controller**: handles HTTP requests/responses, delegates to service
- **Service**: business logic, DTO ↔ Entity mapping
- **Repository**: Spring Data JPA interface, persistence only
- **Entity**: JPA-mapped database model
- **DTO**: request/response shapes, decoupled from the entity
- **Exception handling**: centralized via `@RestControllerAdvice`, consistent error response shape across all endpoints

## Database Schema

## Database Schema Diagram

```text
┌──────────────────────────────────────┐
│              expenses                │
├──────────────────────────────────────┤
│ id (PK)          BIGINT              │
│ title             VARCHAR(100)       │
│ amount            DECIMAL(10,2)      │
│ category          VARCHAR(20)        │
│ expense_date      DATE               │
│ payment_method    VARCHAR(20)        │
│ description       VARCHAR(255)       │
│ created_at        TIMESTAMP          │
└──────────────────────────────────────┘
```

**Indexes:** category, expense_date
## Database Schema Query
```sql
CREATE TABLE expenses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(100)  NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    category        VARCHAR(20)   NOT NULL,
    expense_date    DATE          NOT NULL,
    payment_method  VARCHAR(20)   NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

*(Full setup script in `/sql/schema.sql`)*

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/expenses` | Create an expense |
| GET | `/api/expenses` | Get all expenses (supports `sortBy`, `direction`) |
| GET | `/api/expenses/{id}` | Get one expense by ID |
| PUT | `/api/expenses/{id}` | Update an expense |
| DELETE | `/api/expenses/{id}` | Delete an expense |
| GET | `/api/expenses/category/{category}` | Filter by category |
| GET | `/api/expenses/date-range?startDate=&endDate=` | Filter by date range |
| GET | `/api/expenses/summary` | Total spending + category breakdown |
| GET | `/api/expenses/monthly-summary?year=&month=` | Total spending for a given month |

## Setup Instructions

1. Clone the repository
2. Create the database: run `sql/schema.sql` in your MySQL client
3. Set environment variables: `DB_PASSWORD` (and optionally `DB_USERNAME`, which defaults to `root`)
4. Run the application: `./mvnw spring-boot:run`
5. Open `http://localhost:8081` in your browser

> Note: this project runs on port **8081** instead of the Spring Boot default (8080), configured in `application.properties`.

## Running Tests

```
./mvnw test
```
Includes unit tests (service layer, mocked repository) and integration tests (full request lifecycle, in-memory H2 database).

## Screenshots

### Dashboard
![Dashboard](screenshots/dashboard.png)

### Add Expense
![Add Expense Form](screenshots/add-expense-form.png)

### Filtered View
![Filtered List](screenshots/filtered-list.png)

### Validation Error Handling
![Validation Error](screenshots/validation-error.png)

## Key Design Decisions

- **DTOs instead of exposing entities directly** — decouples API contract from persistence model, prevents clients from setting server-controlled fields like `id`/`createdAt`
- **`BigDecimal` for all money values** — avoids floating-point rounding errors inherent to `double`/`float`
- **Enums for category/payment method** — type safety, no invalid values, self-documenting
- **Centralized exception handling** — consistent error response shape across every endpoint
- **Constructor injection** — testability, immutability, fail-fast on missing dependencies

## Built on Spring Boot 4

This project targets **Spring Boot 4.1.1**, released mid-2026, which introduced a significant modular restructuring of starter dependencies and several package relocations compared to Spring Boot 3.x. Notable migration points handled in this project:

- `spring-boot-starter-web` → `spring-boot-starter-webmvc`
- A single `spring-boot-starter-test` → separate, technology-specific test starters (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-validation-test`, `spring-boot-starter-webmvc-test`)
- Jackson 2's `ObjectMapper` → Jackson 3's `JsonMapper` in test code
- `@AutoConfigureMockMvc` relocated from `org.springframework.boot.test.autoconfigure.web.servlet` to `org.springframework.boot.webmvc.test.autoconfigure`

# fintrack-api

RESTful API for personal finance management built with Java and Spring Boot.

## Features
- JWT Authentication (register/login)
- Account management (checking, savings, cash, investment)
- Transaction tracking with automatic balance updates
- Budget control with spending alerts (warning at 80%, exceeded at 100%)
- Monthly financial summary with category breakdown

## Tech Stack
- Java 21
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA + Hibernate
- PostgreSQL
- Flyway (migrations)
- Docker + Docker Compose
- JUnit 5 + Mockito

## Getting Started

### Prerequisites
- Docker and Docker Compose

### Running with Docker
```bash
git clone https://github.com/igorconrado/fintrack-api
cd fintrack-api
cp .env.example .env
# Edit .env with your values
docker-compose up
```

API will be available at http://localhost:8080

### Running locally
```bash
# Requires Java 21 and PostgreSQL
cp .env.example .env
# Edit .env with your local database credentials
./mvnw spring-boot:run
```

## API Documentation
Swagger UI available at http://localhost:8080/swagger-ui.html

## Endpoints

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | Login and get token |

### Accounts
| Method | Endpoint | Description |
|---|---|---|
| GET | /api/accounts | List all accounts |
| GET | /api/accounts/{id} | Get account by id |
| POST | /api/accounts | Create account |
| PUT | /api/accounts/{id} | Update account |
| DELETE | /api/accounts/{id} | Delete account |

### Transactions
| Method | Endpoint | Description |
|---|---|---|
| GET | /api/transactions | List with filters (type, categoryId, accountId, startDate, endDate, page, size) |
| GET | /api/transactions/{id} | Get transaction by id |
| POST | /api/transactions | Create transaction |
| PUT | /api/transactions/{id} | Update transaction |
| DELETE | /api/transactions/{id} | Delete transaction |

### Categories
| Method | Endpoint | Description |
|---|---|---|
| GET | /api/categories | List all categories |
| POST | /api/categories | Create custom category |
| DELETE | /api/categories/{id} | Delete custom category |

### Budgets
| Method | Endpoint | Description |
|---|---|---|
| GET | /api/budgets | List budgets by period (?month=3&year=2026) |
| POST | /api/budgets | Create budget |
| PUT | /api/budgets/{id} | Update budget |
| DELETE | /api/budgets/{id} | Delete budget |

### Summary
| Method | Endpoint | Description |
|---|---|---|
| GET | /api/summary | Monthly summary (?month=3&year=2026) |

## Environment Variables
```
DB_URL=jdbc:postgresql://localhost:5432/fintrack
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=your-secret-key-minimum-32-characters
JWT_EXPIRATION=86400000
```

## Running Tests
```bash
./mvnw test
```

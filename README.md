# Course Catalog API

A Spring Boot REST API for managing **courses**, **customers**, and **employees** (content creators) across **three separate PostgreSQL databases**. The project demonstrates multi-datasource JPA, cross-database relationship management, orchestrated **saga** workflows with compensating transactions, validation, structured logging with correlation IDs, and OpenAPI documentation.

| | |
|---|---|
| **Java** | 21 |
| **Spring Boot** | 4.0.6 |
| **Build** | Maven |
| **Databases** | PostgreSQL (3 instances) |

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Dependencies](#dependencies)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [How to Run](#how-to-run)
- [API Endpoints](#api-endpoints)
- [Dummy Data Seeding](#dummy-data-seeding)
- [Swagger / OpenAPI](#swagger--openapi)
- [Error Responses](#error-responses)

---

## Features

| Feature | Description |
|---------|-------------|
| **Multi-database** | Course, customer, and employee data each use a dedicated PostgreSQL database and JPA persistence unit (`CourseDBConfig`, `CustomerDBConfig`, `EmployeeDBConfig`). |
| **Domain-driven packages** | Code is organized by bounded context: `course`, `customer`, `employee`, plus shared `common` utilities. |
| **CRUD APIs** | Full create, read, update, and delete for all three domains under versioned paths (`/api/v1/...`). |
| **DTO layer** | Request/response DTOs with **ModelMapper** for entity mapping. |
| **Jakarta Validation** | `@Valid` on request bodies; field-level constraints on DTOs (`@NotBlank`, `@Email`, `@Positive`, etc.). |
| **Global exception handling** | `GlobalExceptionHandler` returns consistent JSON error payloads (404, 400, 409, 500). |
| **Saga pattern** | `CatalogSagaService` + `SagaOrchestrator` coordinate cross-DB operations (enrollment, course creation, deletes) with **compensating steps** on failure. |
| **Cross-DB queries** | Resolve linked entities by ID lists (e.g. customers for a course, courses for an employee). |
| **Pagination & sorting** | `getAll*` endpoints support `page`, `sortBy`, and `sortDir`; page size is configurable per domain via `PageSettings`. |
| **Structured logging** | SLF4J + Logback; controllers and services log key actions at INFO/DEBUG. |
| **Correlation ID** | `CorrelationIdFilter` reads or generates `X-Correlation-Id`, stores it in MDC, echoes it in the response, and includes it in log output. |
| **OpenAPI / Swagger** | springdoc-openAPI with grouped APIs for Courses, Customers, and Employees. |
| **Dummy data seeder** | `CatalogDataSeeder` loads sample employees, customers, and courses on startup (configurable). |
| **Spring Security** | Session-based auth with BCrypt (12), custom login/authorization filters, JDBC-backed persistent sessions, remember-me. |
| **Element collections** | Course–creator and course–customer links stored as `@ElementCollection` ID lists (no cross-DB JPA associations). |

---

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  course_course  │     │ course_customer │     │ course_employee │
│      _db        │     │      _db        │     │      _db        │
│  (courses)      │     │  (customers)    │     │  (employees)    │
└────────▲────────┘     └────────▲────────┘     └────────▲────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │   Course Catalog App   │
                    │  Controllers → Services │
                    │  CatalogSagaService    │
                    └────────────────────────┘
```

**Relationships (logical, not FK across DBs):**

- `Course.contentCreatorIds` → `Employee.id`
- `Course.enrolledCustomerIds` ↔ `Customer.enrolledCourseIds` (kept in sync by saga/seeder)

---

## Project Structure

```
course-catalog/
├── pom.xml
├── mvnw / mvnw.cmd
├── README.md
└── src/
    └── main/
        ├── java/com/koushik/course_catalog/
        │   ├── CourseCatalogApplication.java
        │   ├── course/
        │   │   ├── controller/v1/CourseController.java
        │   │   ├── dto/CourseRequestDTO.java, CourseResponseDTO.java, CreatorAssignmentDTO.java
        │   │   ├── entity/Course.java
        │   │   ├── repository/CourseRepository.java
        │   │   └── service/CourseService.java
        │   ├── customer/
        │   │   ├── controller/v1/CustomerController.java
        │   │   ├── dto/CustomerRequestDTO.java, CustomerResponseDTO.java
        │   │   ├── entity/Customer.java
        │   │   ├── repository/CustomerRepository.java
        │   │   └── service/CustomerService.java
        │   ├── employee/
        │   │   ├── controller/v1/EmployeeController.java
        │   │   ├── dto/EmployeeRequestDTO.java, EmployeeResponseDTO.java
        │   │   ├── entity/Employee.java
        │   │   ├── repository/EmployeeRepository.java
        │   │   └── service/EmployeeService.java
        │   └── common/
        │       ├── config/
        │       │   ├── CatalogDataSeeder.java
        │       │   ├── DataSeedProperties.java
        │       │   ├── ModelMapperConfig.java
        │       │   └── OpenApiConfig.java
        │       ├── dbconfig/
        │       │   ├── CourseDBConfig.java
        │       │   ├── CustomerDBConfig.java
        │       │   └── EmployeeDBConfig.java
        │       ├── entity/PageSettings.java
        │       ├── exception/
        │       │   ├── BadRequestException.java
        │       │   ├── GlobalExceptionHandler.java
        │       │   └── ResourceNotFoundException.java
        │       ├── logging/CorrelationIdFilter.java
        │       └── saga/
        │           ├── CatalogSagaService.java
        │           ├── LinkIdHelper.java
        │           ├── SagaContext.java
        │           ├── SagaException.java
        │           ├── SagaOrchestrator.java
        │           └── SagaStep.java
        ├── security/
        │   ├── authorization/RoleAccessRules.java
        │   ├── config/SecurityConfig.java, SessionConfig.java
        │   ├── controller/AuthController.java
        │   ├── filter/JsonAuthenticationFilter.java, RoleAuthorizationFilter.java
        │   └── service/CatalogUserDetailsService.java
        └── resources/
            ├── application.yml
            ├── application-users.yml
            ├── application.properties
            └── logback-spring.xml
```

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-data-jpa` | JPA / Hibernate persistence |
| `spring-boot-starter-webmvc` | REST controllers |
| `spring-boot-starter-validation` | Jakarta Bean Validation |
| `postgresql` (runtime) | PostgreSQL JDBC driver |
| `lombok` | Boilerplate reduction (`@Data`, constructors) |
| `modelmapper` (3.2.0) | DTO ↔ entity mapping |
| `springdoc-openapi-starter-webmvc-ui` (2.5.0) | Swagger UI & OpenAPI docs |
| `spring-boot-starter-security` | Authentication & authorization |
| `spring-session-jdbc` | Persistent HTTP sessions in PostgreSQL |
| `spring-boot-devtools` (optional, runtime) | Dev-time reload |

---

## Prerequisites

- **JDK 21**
- **Maven 3.9+** (or use included `./mvnw`)
- **PostgreSQL** with three databases and users (see below)

---

## Database Setup

Create three databases and users (adjust names/passwords to match `application.yml`):

| Database | URL | Username | Password |
|----------|-----|----------|----------|
| `course_course_db` | `jdbc:postgresql://localhost:5432/course_course_db` | `course` | `course` |
| `course_customer_db` | `jdbc:postgresql://localhost:5432/course_customer_db` | `cust` | `cust` |
| `course_employee_db` | `jdbc:postgresql://localhost:5432/course_employee_db` | `emp` | `emp` |

Example (psql as superuser):

```sql
CREATE ROLE course WITH LOGIN PASSWORD 'course';
CREATE DATABASE course_course_db OWNER course;

CREATE ROLE cust WITH LOGIN PASSWORD 'cust';
CREATE DATABASE course_customer_db OWNER cust;

CREATE ROLE emp WITH LOGIN PASSWORD 'emp';
CREATE DATABASE course_employee_db OWNER emp;
```

If these roles already exist but cannot log in, run:

```sql
ALTER ROLE course WITH LOGIN;
ALTER ROLE cust WITH LOGIN;
ALTER ROLE emp WITH LOGIN;
```

Hibernate `ddl-auto: update` creates/updates tables on startup.

---

## Configuration

Main settings in `src/main/resources/application.yml`:

```yaml
app:
  data:
    seed:
      enabled: true        # Load dummy data on startup
      only-if-empty: true  # Skip if any DB already has rows
      employees: 20
      customers: 50
      courses: 30

spring:
  datasource:
    course:   # course DB connection
    customer: # customer DB connection
    employee: # employee DB connection
```

---

## How to Run

### 1. Build

```bash
mvn clean compile
```

### 2. Run the application

```bash
mvn spring-boot:run
```

Or with the wrapper:

```bash
./mvnw spring-boot:run        # Linux/macOS
mvnw.cmd spring-boot:run      # Windows
```

### 3. Log in (required for API calls)

See **[USERS-CREDENTIALS.md](USERS-CREDENTIALS.md)** for usernames and passwords.

```bash
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"Admin@123\"}"
```

Use `-b cookies.txt` on subsequent requests.

### 4. Verify

- API base: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 4. Optional: send a correlation ID

```bash
curl -H "X-Correlation-Id: my-trace-123" http://localhost:8080/api/v1/courses/getAllCourses
```

The same ID appears in response headers and logs.

---

## API Endpoints

Base URL: `http://localhost:8080`  
**Authentication:** Session cookie (`CATALOG_SESSION`) required except for login and Swagger.

### Auth — `/api/v1/auth`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/login` | JSON body `{ "username", "password" }` — creates persistent session |
| `POST` | `/logout` | Ends session |
| `GET` | `/me` | Current user details |

### Courses — `/api/v1/courses`

| Method | Endpoint | Description | Saga |
|--------|----------|-------------|------|
| `POST` | `/addCourse` | Create course (validates creators/customers, links enrollments) | Yes |
| `PUT` | `/updateCourse/{id}` | Update course; enrollment/creator changes use saga | Partial |
| `DELETE` | `/deleteCourse/{id}` | Unlink customers, then delete course | Yes |
| `GET` | `/getCourse/{id}` | Get one course | No |
| `GET` | `/getAllCourses?page=0&sortBy=id&sortDir=asc` | Paginated list with sorting | No |
| `POST` | `/setRecordsinPage?size=10` | Set page size for course list | No |
| `GET` | `/getCustomersByCourse/{courseId}` | Enrolled customers for a course | No |
| `GET` | `/getCreatorsByCourse/{courseId}` | Content creators (employees) for a course | No |
| `POST` | `/enrollCustomer/{courseId}/{customerId}` | Enroll customer (both DBs) | Yes |
| `DELETE` | `/unenrollCustomer/{courseId}/{customerId}` | Unenroll customer | Yes |
| `PUT` | `/assignCreators/{courseId}` | Assign creator employee IDs (body below) | Yes |

**Assign creators body:**

```json
{
  "contentCreatorIds": [1, 2, 3]
}
```

---

### Customers — `/api/v1/customers`

| Method | Endpoint | Description | Saga |
|--------|----------|-------------|------|
| `POST` | `/addCustomer` | Create customer | No |
| `PUT` | `/updateCustomer/{id}` | Update customer | No |
| `DELETE` | `/deleteCustomer/{id}` | Unlink from courses, then delete | Yes |
| `GET` | `/getCustomer/{id}` | Get one customer | No |
| `GET` | `/getAllCustomers?page=0&sortBy=id&sortDir=asc` | Paginated list with sorting | No |
| `POST` | `/setRecordsinPage?size=10` | Set page size for customer list | No |
| `POST` | `/enrollInCourse/{customerId}/{courseId}` | Enroll in course (both DBs) | Yes |

---

### Employees — `/api/v1/employees`

| Method | Endpoint | Description | Saga |
|--------|----------|-------------|------|
| `POST` | `/addEmployee` | Create employee (content creator) | No |
| `PUT` | `/updateEmployee/{id}` | Update employee | No |
| `DELETE` | `/deleteEmployee/{id}` | Unlink from courses as creator, then delete | Yes |
| `GET` | `/getEmployee/{id}` | Get one employee | No |
| `GET` | `/getAllEmployees?page=0&sortBy=id&sortDir=asc` | Paginated list with sorting | No |
| `POST` | `/setRecordsinPage?size=10` | Set page size for employee list | No |
| `GET` | `/getCoursesByEmployee/{employeeId}` | Courses where employee is a creator | No |

---

### Pagination query parameters

Used by all `getAll*` endpoints:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | `0` | Zero-based page index |
| `sortBy` | `id` | Entity field to sort on |
| `sortDir` | `asc` | `asc` or `desc` |

Page size defaults to **5** per domain until changed via `setRecordsinPage`.

---

## Dummy Data Seeding

When `app.data.seed.enabled=true` and databases are empty, startup loads:

| Entity | Count | Notes |
|--------|-------|-------|
| Employees | 20 | Emails `employee1@course-catalog.test` … `employee20@...` |
| Customers | 50 | Emails `customer1@course-catalog.test` … `customer50@...` |
| Courses | 30 | Titles like `Java Fundamentals (1)`, `Spring Boot (2)`, … |

Each course gets 1–3 random creators and 3–12 random enrolled customers (bidirectional links).

To **re-seed**, truncate tables in all three databases and restart the app.

---

## Swagger / OpenAPI

Grouped API documentation:

- **Courses** — `/api/v1/courses/**`
- **Customers** — `/api/v1/customers/**`
- **Employees** — `/api/v1/employees/**`

Open `http://localhost:8080/swagger-ui.html` to explore and try endpoints interactively.

---

## Error Responses

| HTTP Status | When |
|-------------|------|
| `400` | Validation failure (`MethodArgumentNotValidException`) or `BadRequestException` |
| `404` | `ResourceNotFoundException` (entity not found) |
| `409` | `SagaException` (distributed workflow failed; compensations attempted) |
| `500` | Unhandled exceptions |

Example error body:

```json
{
  "timestamp": "2026-05-20T10:15:30",
  "status": 404,
  "message": "Course not found with id: 99"
}
```

Validation errors return an `errors` map with field names and messages.

---

## Saga workflows (summary)

| Saga | Triggered by |
|------|----------------|
| **CreateCourse** | `POST addCourse` |
| **DeleteCourse** | `DELETE deleteCourse` |
| **EnrollCustomer** | `POST enrollCustomer`, `POST enrollInCourse` |
| **UnenrollCustomer** | `DELETE unenrollCustomer` |
| **AssignCreators** | `PUT assignCreators`, creator update on `updateCourse` |
| **DeleteCustomer** | `DELETE deleteCustomer` |
| **DeleteEmployee** | `DELETE deleteEmployee` |

On failure, completed steps run **compensating actions** in reverse order (e.g. restore enrollment lists or delete a partially created course).

---

## Security

| Component | Purpose |
|-----------|---------|
| `JsonAuthenticationFilter` | JSON login at `POST /api/v1/auth/login` |
| `RoleAuthorizationFilter` | Role-based access control per API path |
| `CatalogUserDetailsService` | Loads users from `application-users.yml` |
| `SessionConfig` | JDBC session store + remember-me tokens in course DB |
| BCrypt strength **12** | Password hashing |

**Roles:** `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_EMPLOYEE`, `ROLE_CUSTOMER` — see [USERS-CREDENTIALS.md](USERS-CREDENTIALS.md).

---

## Running Tests

```bash
mvn test
```

Test suite (**48 tests**) uses JUnit 5, Mockito, AssertJ, and standalone MockMvc (no live PostgreSQL required).

| Area | Test classes |
|------|----------------|
| Saga | `SagaOrchestratorTest`, `CatalogSagaServiceTest`, `LinkIdHelperTest` |
| Services | `CourseServiceTest`, `CustomerServiceTest`, `EmployeeServiceTest` |
| Controllers | `CourseControllerTest`, `CustomerControllerTest`, `EmployeeControllerTest` |
| Cross-cutting | `GlobalExceptionHandlerTest`, `CorrelationIdFilterTest` |
| Security | `RoleAccessRulesTest`, `CatalogUserDetailsServiceTest` |

Seeding is disabled in tests via `src/test/resources/application.properties`.

---

## License

This project is for learning and demonstration purposes.

# Course Catalog — Application User Credentials

These accounts are used for **HTTP session authentication** against the Course Catalog API.  
Passwords are stored in the application as **BCrypt hashes (strength 12)** in `src/main/resources/application-users.yml`.

> **Security notice:** Change these passwords before any production deployment. Do not commit real production secrets to source control.

---

## Login endpoint

```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "<username>",
  "password": "<password>"
}
```

On success, the server returns **200 OK** and sets a **session cookie** (`CATALOG_SESSION`).  
Send that cookie on subsequent API requests. Optional **remember-me** cookie (`CATALOG_REMEMBER_ME`) keeps you signed in for 7 days.

```http
POST http://localhost:8080/api/v1/auth/logout
```

```http
GET http://localhost:8080/api/v1/auth/me
```

---

## User accounts

| Username | Password | Role | Access |
|----------|----------|------|--------|
| `admin` | `Admin@123` | **ADMIN** | Full access to courses, customers, and employees |
| `manager` | `Manager@123` | **MANAGER** | Full CRUD on courses, customers, and employees |
| `employee` | `Employee@123` | **EMPLOYEE** | Read-only: courses (GET) and employees (GET) |
| `customer` | `Customer@123` | **CUSTOMER** | Read-only: courses & customers (GET); enroll/unenroll |

---

## Role summary

| Role | Courses | Customers | Employees |
|------|---------|-----------|-----------|
| ADMIN | All operations | All operations | All operations |
| MANAGER | All operations | All operations | All operations |
| EMPLOYEE | GET only | — | GET only |
| CUSTOMER | GET + enroll/unenroll | GET + enroll | — |

---

## Example (curl)

```bash
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"Admin@123\"}"

curl -b cookies.txt http://localhost:8080/api/v1/courses/getAllCourses
```

---

## Session persistence

- Sessions are stored in the **course database** (`course_course_db`) using **Spring Session JDBC**.
- Default inactivity timeout: **8 hours** (see `application.yml`).
- Session cookie max-age: **7 days** (browser may keep the cookie; server still enforces inactivity timeout).
- **Remember-me** is enabled for long-lived sign-in across browser restarts.

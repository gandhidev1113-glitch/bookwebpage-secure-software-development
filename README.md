# bookwebpage-secure-software-development

# 🔐 Book Webpage — Secure Software Development

## 📌 Project Overview

This project is a secure web application developed as part of a **Secure Software Development** course.
It focuses on identifying, exploiting, and mitigating common web vulnerabilities following the **OWASP Top 10**.

The project is built collaboratively, emphasizing **secure coding practices, team workflow, and application security testing**.

---

## 🎯 Objectives

* Identify real-world security vulnerabilities
* Perform controlled exploitation (OWASP Juice Shop)
* Apply secure coding techniques
* Implement secure API design principles
* Collaborate using Git and GitHub workflow

---

## 👥 Team Collaboration

This project is developed in a team environment using GitHub:

* Feature-based branching (`feature/*`)
* Pull Requests for code review
* Continuous updates via `git pull`
* Secure and clean commit practices

---

## 🧪 Security Findings

### 🔓 1. Broken Access Control (IDOR)

* Access to other users’ data by modifying IDs
* Missing server-side ownership checks

### 💉 2. SQL Injection (Authentication Bypass)

* Payload: `' OR 1=1--`
* Result: Unauthorized admin access

### ⚡ 3. DOM-Based XSS

* Injection via URL (`location.hash`)
* JavaScript execution in browser context

---

## 🛠️ OWASP Top 10 Mapping

| Vulnerability | OWASP Category             |
| ------------- | -------------------------- |
| IDOR          | A01: Broken Access Control |
| SQL Injection | A03: Injection             |
| XSS           | A03: Injection             |

---

## 🔐 Secure Coding Improvements

* ✅ Parameterized queries (SQL Injection prevention)
* ✅ Server-side authorization checks (IDOR protection)
* ✅ Output encoding (XSS prevention)
* ✅ Avoid `innerHTML` with untrusted input
* ✅ CSRF protection (tokens + origin validation)
* ✅ SSRF protection (URL validation + allowlisting)

---

## 🌐 Secure API Design

### Example Endpoint

`GET /api/users/{id}/orders`

### Security Controls

* Authentication (JWT validation)
* Authorization (RBAC + ownership checks)
* Deny-by-default access model
* Input validation (strict schema)
* Data minimization (DTOs)
* Rate limiting & abuse prevention
* Logging & monitoring

---

## ⚙️ Tech & Tools

* OWASP Juice Shop
* JavaScript / Node.js
* Git & GitHub
* Browser DevTools
* OWASP Top 10 Framework

---

## 🧠 Key Takeaways

* Never trust client input
* Always enforce server-side security
* Authentication ≠ Authorization
* Use secure defaults (deny-by-default)
* Protect sensitive data (tokens, passwords)
* Apply security early (Shift Left)

---

## 🚀 Getting Started

```bash
git clone https://github.com/gandhidev1113-glitch/bookwebpage-secure-software-development.git
cd bookwebpage-secure-software-development
git pull origin main
```

---

## 🔄 Workflow

```bash
git checkout -b feature/your-feature-name
git add .
git commit -m "Your message"
git push origin feature/your-feature-name
```

Create a Pull Request for review.

---

## 👤 Contributors

**Devkumar Parikshit GANDHI**
**Thai Bao DUONG**
**Sofyen FENICH**
**Arthur Amuda**


---

## 📜 License

This project is for academic and educational purposes only.

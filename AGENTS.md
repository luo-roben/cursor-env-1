# AGENTS.md

## Cursor Cloud specific instructions

### Project Overview
The `compliance-review/` directory contains a Spring Boot 3.2.5 (Java 21) backend for an intelligent compliance review system. It uses MySQL 8 as its sole data store and a mock LLM model for MVP.

### Prerequisites
- Java 21 and Maven 3.8+ are pre-installed in the VM.
- MySQL 8 must be running. Start with `sudo service mysql start`.
- Database and user must exist (see below).

### Database Setup (one-time)
```bash
sudo service mysql start
sudo mysql -u root -e "
  CREATE DATABASE IF NOT EXISTS compliance_review DEFAULT CHARSET utf8mb4;
  CREATE DATABASE IF NOT EXISTS compliance_review_test DEFAULT CHARSET utf8mb4;
  CREATE USER IF NOT EXISTS 'compliance'@'localhost' IDENTIFIED BY 'compliance123';
  GRANT ALL PRIVILEGES ON compliance_review.* TO 'compliance'@'localhost';
  GRANT ALL PRIVILEGES ON compliance_review_test.* TO 'compliance'@'localhost';
  FLUSH PRIVILEGES;
"
```

### Common Commands
All commands run from `/workspace/compliance-review/`:

| Action | Command |
|---|---|
| Compile | `mvn compile` |
| Unit tests | `mvn test` |
| Run app (dev) | `mvn spring-boot:run` |
| Package | `mvn package -DskipTests` |

The app starts on port **8080**. Schema auto-initializes via `spring.sql.init` from `src/main/resources/db/schema.sql`.

### Key Gotchas
- The JDBC URL must use `characterEncoding=UTF-8` (not `utf8mb4`) — the MySQL Connector/J driver does not recognize `utf8mb4` as a Java charset.
- The `spring.sql.init.mode=always` means schema.sql runs on every startup; all DDL uses `CREATE TABLE IF NOT EXISTS` and `ON DUPLICATE KEY UPDATE` so it's safe for repeated runs.
- JSON columns in entities are mapped as `String`; serialize/deserialize manually with Jackson `ObjectMapper`.
- The AI module uses a `MockChatModel` that detects keywords like "保本", "收益率", "稳赚" to generate realistic violation results without an actual LLM.
- The `/api/v1/review/upload` endpoint requires `tesseract-ocr` with Chinese language support for image OCR. Install via: `sudo apt-get install -y tesseract-ocr tesseract-ocr-chi-sim tesseract-ocr-chi-tra`. Without tesseract, image uploads still work but return a placeholder OCR text.
- The MySQL socket directory `/var/run/mysqld/` may have restrictive permissions after service start; run `sudo chmod 755 /var/run/mysqld/` if you get socket connection errors from non-root users.

### API Exploration
Swagger UI is available at `http://localhost:8080/swagger-ui.html` and OpenAPI spec at `/v3/api-docs`.

### Frontend (compliance-review-web/)
The `compliance-review-web/` directory contains a Vite + React 18 + TypeScript frontend using Ant Design 5.x.

| Action | Command |
|---|---|
| Install deps | `npm install` (from `compliance-review-web/`) |
| Dev server | `npm run dev` (port 3000, proxies `/api` → `localhost:8080`) |
| Lint | `npm run lint` |
| Build | `npm run build` |

- The dev server proxies all `/api` requests to the Spring Boot backend at port 8080. Start the backend first.
- The `react-hooks/set-state-in-effect` ESLint rule is disabled because data fetching in effects with setState is a standard React 18 pattern.

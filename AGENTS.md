# AGENTS.md

## Cursor Cloud specific instructions

### Tech Stack
- **Language:** Java 21 (OpenJDK)
- **Build Tool:** Apache Maven 3.8.7
- **Testing:** JUnit 5 (Jupiter)

### Project Structure
- Maven project lives under `app/` directory.
- Source code: `app/src/main/java/`
- Tests: `app/src/test/java/`

### Common Commands
All commands should be run from the `app/` directory:

| Task | Command |
|------|---------|
| Compile | `mvn compile` |
| Run tests | `mvn test` |
| Build JAR | `mvn package` |
| Run app | `mvn exec:java -Dexec.mainClass="com.example.App"` |
| Run JAR | `java -cp target/app-1.0-SNAPSHOT.jar com.example.App` |

### Notes
- Maven is installed system-wide via `apt`. The update script reinstalls it if missing.
- No lint tool is currently configured. Consider adding Checkstyle or SpotBugs if code quality checks are needed.
- No external services (databases, caches, etc.) are required.

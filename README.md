# CraveReel

CraveReel is a Java web application for discovering recipes through short cooking reels. Users can browse recipes, search and filter the catalogue, view ingredients and cooking steps, watch reels, save recipes, like and rate recipes, comment, and use role-based dashboards.

## Technology

- Java 21
- Spring Boot 3.2
- Spring MVC / REST
- Spring Data JPA and Hibernate
- MySQL 8
- JDBC / Spring JdbcTemplate
- Spring Security + JWT
- HTML, CSS and JavaScript
- Maven

## Project structure

```text
src/main/java/com/recipereels/
├── config/        Security and web configuration
├── controller/    HTTP/REST endpoints
├── dto/           Request and response objects
├── entity/        Database entities
├── exception/     Application exceptions and error handling
├── repository/    Database repositories
├── security/      Authentication and JWT handling
├── service/       Business-service interfaces
├── service/impl/  Service implementations
├── analytics/     Recipe/reel analytics
├── servlet/       Servlet-based status endpoint
└── util/          Application initialization helpers

src/main/resources/static/
├── css/
├── js/
└── *.html
```

## Requirements

- JDK 21
- Maven 3.9+
- MySQL 8+
- Git

## Database setup

Create the database in MySQL:

```sql
CREATE DATABASE recipereels;
```

The application uses Hibernate's `ddl-auto=update`, so the tables are created/updated when the application starts.

If you are using an existing CraveReel database, import your database dump before starting the application.

## Environment variables

Database credentials and the JWT signing key are intentionally not stored in the repository.

PowerShell example:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
$env:JWT_SECRET="YOUR_BASE64_JWT_SECRET"
```

See `.env.example` for the required variable names.

## Run locally

From the project root:

```powershell
mvn spring-boot:run
```

If Maven is installed at a custom location on Windows:

```powershell
& "$env:USERPROFILE\Downloads\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.fork=false"
```

Then open:

```text
http://localhost:8080/
```

## Useful endpoints

- `/` — CraveReel home page
- `/login.html` — login page
- `/explorer-dashboard.html` — explorer dashboard
- `/api/reels` — approved reels
- `/api/analytics/top-reels` — top reel analytics
- `/system/status` — application status servlet

## Database and Java integration

Most application data is handled through Spring Data repositories and JPA/Hibernate. CraveReel also uses `JdbcTemplate` for the reel analytics query, providing direct JDBC-based database access where a simple SQL report is useful.

## Collaboration

Use feature branches instead of committing directly to `main`:

```bash
git checkout -b feature-name
git add .
git commit -m "Describe the change"
git push -u origin feature-name
```

Then open a pull request on GitHub.

## Security

Do not commit `.env`, local application overrides, database passwords, JWT secrets, or generated build files. Use environment variables for local and deployment credentials.

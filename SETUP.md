# BankingApplication Setup

## Installed / verified

- Java 23 is installed
- Maven 3.9.14 is installed
- MySQL Server 8.0 is installed and running
- Backend Maven dependencies were downloaded into `BankingApplication_Backend-main/.m2`

## Backend

From `BankingApplication_Backend-main`:

```powershell
mvn "-Dmaven.repo.local=$PWD\.m2" test
```

To run the API server:

```powershell
mvn "-Dmaven.repo.local=$PWD\.m2" exec:java
```

The API listens on `http://localhost:8080`.

## Database

Initialize the database and application user with a MySQL admin account:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < .\db\init.sql
```

Default app database settings after init:

- `BANK_DB_HOST=localhost`
- `BANK_DB_PORT=3306`
- `BANK_DB_NAME=BankingApplication`
- `BANK_DB_USER=bank_app`
- `BANK_DB_PASSWORD=bank_app`

If you want different credentials, set these environment variables before starting the backend.

## Frontend

The frontend is static. Open `BankingApplication_Frontend-main/index.html` in a browser after the backend is running.

## Notes

- The frontend now supports create account, deposit, withdraw, transfer, view account, and list accounts.
- Database writes are used for transaction logging. If the database is not initialized, the app still runs and skips DB transaction logging.
- Email alerts are optional. Set `BANK_ALERT_FROM_EMAIL` and `BANK_ALERT_APP_PASSWORD` if you want low-balance email notifications enabled.

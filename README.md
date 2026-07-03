# PoeTicketQueue

A ticket queue web app for Path of Exile groups.

## Prerequisites

- Java 21+
- Maven 3.8+
- Node.js 18+ and npm

## Running Locally

Both the backend and frontend need to run at the same time in separate terminals.

### Backend

```bash
cd backend
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on `http://localhost:5173` and proxies `/api` requests to the backend automatically.

## Project Structure

```
PoeTicketQueue/
├── backend/    # Spring Boot 3.3 (Java 21)
└── frontend/   # Vue 3 + Vite 5
```

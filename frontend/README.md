# TaskTracker frontend

Для локального запуска нужны Node.js 24, Java 25 и PostgreSQL. Скопируйте `.env.example` в `.env` в корне репозитория и задайте собственный `JWT_SECRET`. Если локальная БД ещё не запущена, поднимите её из корня проекта.

```powershell
docker compose up -d postgres
cd backend
.\mvnw.cmd spring-boot:run
```

В другом терминале из корня проекта:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd run dev
```

Откройте страницу по адресу, который напечатает Vite. По умолчанию запросы `/api/*` проксируются на `http://127.0.0.1:8080`. Если Spring Boot запущен на другом порту, задайте `TASKTRACKER_API_TARGET` перед запуском Vite.

`npm.cmd run build` проверяет TypeScript и собирает `dist/`. Для размещения сборки настройте прокси на том же origin, который перенаправляет `/api/*` на Spring Boot и снимает префикс `/api`.

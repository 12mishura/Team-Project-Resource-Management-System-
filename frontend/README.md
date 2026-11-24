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

## Проверки качества

| Команда                               | Что делает                                             |
| ------------------------------------- | ------------------------------------------------------ |
| `npm.cmd run typecheck`               | TypeScript для приложения и e2e-тестов                 |
| `npm.cmd run lint`                    | ESLint (TypeScript, React Hooks, доступность jsx-a11y) |
| `npm.cmd run format` / `format:check` | Prettier: форматирует или только проверяет             |
| `npm.cmd test`                        | Unit- и компонентные тесты на Vitest и Testing Library |
| `npm.cmd run test:e2e`                | Сценарии Playwright в Chromium                         |
| `npm.cmd run check`                   | typecheck, lint, format:check и unit-тесты разом       |

Перед первым запуском e2e установите браузер: `npx.cmd playwright install chromium`. E2E-тесты работают с настоящим бэкендом: он должен быть запущен на порту 8080 вместе с PostgreSQL. Vite Playwright поднимет сам или использует уже запущенный на порту 5173. Каждый прогон создаёт новых пользователей и организации с уникальными именами, поэтому запускать тесты лучше на локальной или тестовой базе.

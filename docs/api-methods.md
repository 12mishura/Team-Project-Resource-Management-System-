# TaskTracker — список методов API

Обозначения:
- ✅ — уже есть и покрыто тестами
- 🔴 — не хватает, важно (без этого приложением неудобно или нельзя пользоваться)
- 🟡 — не хватает, желательно
- ⚪ — можно позже

Доступ: **все** — без токена; **токен** — любой залогиненный; **участник** — член организации (OWNER/ADMIN/USER).

Пути участников и проектов начинаются с `/organizations/{organizationId}`, ниже он сокращён до `/{orgId}`.

---

## 1. Аутентификация — `AuthController` / `AuthService`

| | HTTP | Путь | Доступ | Метод сервиса |
|---|---|---|---|---|
| ✅ | POST | `/auth/register` | все | `register` |
| ✅ | POST | `/auth/login` | все | `login` |
| ✅ | GET | `/auth/me` | токен | `getCurrentUser` |
| ✅ | POST | `/auth/refresh` | все (по refresh-токену) | `refresh` — новая пара токенов; старый refresh отзывается; повторное использование отозванного → отзыв всех сессий |
| ✅ | POST | `/auth/logout` | все (по refresh-токену) | `logout` — отзывает refresh-токен, идемпотентно |

Login и refresh возвращают `accessToken` (1 час), `refreshToken` (14 дней, в базе хранится только SHA-256 хеш), `expiresIn`, `refreshExpiresIn`.

## 2. Профиль пользователя — `UserController` / `UserService`

| | HTTP | Путь | Доступ | Что делает |
|---|---|---|---|---|
| ✅ | PATCH | `/users/me` | токен | `updateProfile` — смена `username` (уникален без учёта регистра) |
| ✅ | PUT | `/users/me/password` | токен | `changePassword` — проверка текущего пароля; после смены все старые access- и refresh-токены перестают работать |
| ✅ | GET | `/users/me/issues?type=&status=&parentId=&q=&sortBy=&direction=&page=&size=` | токен | `getMyIssues` — мои задачи по всем организациям |
| ✅ | GET | `/users/me/worklogs?from=&to=` | токен | `WorkLogService.getTimesheet` — мои списания времени за период (даты `YYYY-MM-DD`, не длиннее 92 дней) с организацией, проектом и задачей |
| ⚪ | DELETE | `/users/me` | токен | удаление аккаунта (сначала решить, что с организациями, где он OWNER) |

## 3. Организации — `OrganizationController` / `OrganizationService`

| | HTTP | Путь | Доступ | Метод сервиса |
|---|---|---|---|---|
| ✅ | POST | `/organizations` | токен → становится OWNER | `createOrganization` |
| ✅ | GET | `/organizations?page=&size=` | токен | `getMyOrganizations` — мои организации, моя роль, `memberId`, дата вступления |
| ✅ | GET | `/organizations/{id}` | участник | `getOrganizationById` |
| ✅ | PATCH | `/organizations/{id}` | OWNER, ADMIN | `updateOrganization` |
| ✅ | DELETE | `/organizations/{id}` | OWNER | `deleteOrganization` |
| ✅ | GET | `/organizations/{id}/overview` | участник | `OrganizationOverviewService.getOverview` — число проектов и участников, открытые задачи и мои задачи; по каждому доступному проекту: задачи по статусам, открытые баги, «на мне», участники (до 5 имён). OWNER / ADMIN видят все проекты, USER — только свои |
| ✅ | GET | `/organizations/{id}/invite-code` | OWNER, ADMIN | `getInviteCode` |
| ✅ | POST | `/organizations/{id}/invite-code/regenerate` | OWNER, ADMIN | `regenerateInviteCode` — старый код сразу перестаёт работать |
| ✅ | POST | `/organizations/{id}/transfer-ownership` | OWNER | `transferOwnership` — новый владелец становится OWNER, старый ADMIN; в базе гарантирован ровно один OWNER |

## 4. Участники — `OrganizationMemberController` / `OrganizationMemberService`

| | HTTP | Путь | Доступ | Метод сервиса |
|---|---|---|---|---|
| ✅ | POST | `/organizations/join` | токен | `joinToOrganization` |
| ✅ | GET | `/{orgId}/members?page=&size=` | участник | `getMembers` |
| ✅ | PATCH | `/{orgId}/members/{memberId}` | OWNER, ADMIN + правила | `updateRole` |
| ✅ | DELETE | `/{orgId}/members/{memberId}` | OWNER, ADMIN + правила | `removeMember` |
| ✅ | DELETE | `/{orgId}/members/me` | любой, кроме OWNER | `leaveOrganization` |
| ✅ | PATCH | `/{orgId}/members/{memberId}/capacity` | OWNER, ADMIN | `WorkloadService.updateCapacity` — `weeklyCapacityMinutes` от 0 до 10080 (по умолчанию 2400 = 40 ч); поле есть в ответах списка участников |
| ✅ | GET | `/{orgId}/workload?from=&to=` | OWNER, ADMIN | `WorkloadService.getOrganizationWorkload` — по каждому участнику организации: часы по дням и по проектам, ёмкость за период (рабочие дни × недельная ёмкость / 5), открытые назначенные задачи, задачи без оценки, оставшаяся оценка, флаг `overloaded` (остаток больше недельной ёмкости) |

## 5. Проекты — `ProjectController` / `ProjectService`

| | HTTP | Путь | Доступ | Метод сервиса |
|---|---|---|---|---|
| ✅ | POST | `/{orgId}/projects` | OWNER, ADMIN | `createProject` |
| ✅ | GET | `/{orgId}/projects?status=&page=&size=` | участник | `getProjects` |
| ✅ | GET | `/{orgId}/projects/{projectId}` | участник | `getProject` |
| ✅ | PATCH | `/{orgId}/projects/{projectId}` | OWNER, ADMIN организации или ADMIN проекта | `updateProject` |
| ✅ | PATCH | `/{orgId}/projects/{projectId}/status` | OWNER, ADMIN организации или ADMIN проекта | `changeStatus` |
| ✅ | DELETE | `/{orgId}/projects/{projectId}` | OWNER | `deleteProject` |
| ✅ | GET | `/{orgId}/projects/{projectId}/workload?from=&to=` | OWNER, ADMIN организации или ADMIN проекта | `WorkloadService.getProjectWorkload` — то же, что загрузка организации, но по участникам проекта и только по времени в этом проекте |
| ✅ | GET | `/{orgId}/projects/{projectId}/activity?page=&size=` | участник проекта | `IssueService.getProjectActivity` — лента событий всех задач проекта, новые сверху, с названием и типом задачи |

## 5.1. Участники проекта — `ProjectMemberController` / `ProjectMemberService`

Путь: `/{orgId}/projects/{projectId}/members`. Роли в проекте: `ADMIN` / `EXTENDED` / `USER`. Добавить можно только участника этой же организации.

| | HTTP | Путь | Доступ | Что делает |
|---|---|---|---|---|
| ✅ | POST | `/members` | OWNER, ADMIN организации или ADMIN проекта | `addMember` |
| ✅ | GET | `/members?page=&size=` | участник организации | `getMembers` |
| ✅ | PATCH | `/members/{projectMemberId}` | OWNER, ADMIN организации или ADMIN проекта (свою роль ADMIN проекта менять не может) | `updateRole` |
| ✅ | DELETE | `/members/{projectMemberId}` | OWNER, ADMIN организации или ADMIN проекта (удалить себя ADMIN проекта не может) | `removeMember` |

## 6. Задачи — `IssueController` / `IssueService`

Путь: `/{orgId}/projects/{projectId}/issues`. Абстрактный `Issue` (SINGLE_TABLE) и наследники `Epic`, `Story` (`storyPoints`), `Task`, `Bug` (`severity`, `stepsToReproduce`). Только Epic может быть родителем; Epic не может иметь родителя.

| | HTTP | Путь | Метод сервиса |
|---|---|---|---|
| ✅ | POST | `/issues` | `createIssue` — тип в поле `type`; `parentId` сразу при создании — только EXTENDED / ADMIN; `assigneeId` (id участника проекта) — по тем же правилам, что и назначение: USER может указать только себя |
| ✅ | GET | `/issues?type=&status=&assigneeId=&parentId=&q=&sortBy=&direction=&page=&size=` | `getIssues` — `q`: поиск по названию; `sortBy`: `CREATED_AT` / `UPDATED_AT` / `PRIORITY` / `STATUS`; `direction`: `ASC` / `DESC` |
| ✅ | GET | `/issues/{issueId}` | `getIssue` |
| ✅ | PATCH | `/issues/{issueId}` | `updateIssue` |
| ✅ | PATCH | `/issues/{issueId}/status` | `changeStatus` — только по workflow (см. ниже) |
| ✅ | PATCH | `/issues/{issueId}/assignee` | `assignIssue` — `projectMemberId` обязателен |
| ✅ | PATCH | `/issues/{issueId}/assignee/me` | `assignToMe` — назначить себя; нужно быть участником проекта (OWNER / ADMIN организации добавляют себя на вкладке Members); USER — только на свободную задачу |
| ✅ | DELETE | `/issues/{issueId}/assignee` | `unassignIssue` — снять исполнителя |
| ✅ | PATCH | `/issues/{issueId}/parent` | `changeParent` — `parentId` обязателен |
| ✅ | DELETE | `/issues/{issueId}/parent` | `detachFromEpic` — отвязать от Epic |
| ✅ | DELETE | `/issues/{issueId}` | `deleteIssue` |
| ✅ | GET | `/issues/{issueId}/history?page=&size=` | `getHistory` — CREATED, FIELD_CHANGED (в т.ч. `estimate`), STATUS_CHANGED, ASSIGNEE_CHANGED, PARENT_CHANGED, WORK_LOGGED со старым и новым значением и автором |

**Оценка и время в задаче:** `estimateMinutes` (1…60000) передаётся при создании и в `PATCH /issues/{issueId}`; `0` убирает оценку. У Epic своей оценки нет (400): `estimateMinutes`, `spentMinutes` и `remainingMinutes` эпика считаются по его задачам. В ответе задачи: `estimateMinutes`, `spentMinutes` (сумма списаний), `remainingMinutes` (`max(оценка − списано, 0)` или `null` без оценки).

## 6.1. Комментарии — `IssueCommentController` / `IssueCommentService`

Путь: `/{orgId}/projects/{projectId}/issues/{issueId}/comments`.

| | HTTP | Путь | Доступ | Метод сервиса |
|---|---|---|---|---|
| ✅ | POST | `/comments` | кто видит задачу | `addComment` |
| ✅ | GET | `/comments?page=&size=` | кто видит задачу | `getComments` |
| ✅ | PATCH | `/comments/{commentId}` | только автор | `editComment` — ставит `editedAt` |
| ✅ | DELETE | `/comments/{commentId}` | автор или ADMIN проекта | `deleteComment` |

## 6.2. Учёт времени — `WorkLogController` / `WorkLogService`

Путь: `/{orgId}/projects/{projectId}/issues/{issueId}/worklogs`.

| | HTTP | Путь | Доступ | Метод сервиса |
|---|---|---|---|---|
| ✅ | POST | `/worklogs` | участник проекта (OWNER / ADMIN организации сначала добавляют себя в проект) | `addWorkLog` — `minutes` 1…1440, `workDate` не позже завтрашнего дня (UTC), `comment` до 500 символов; на Epic нельзя (400); всего за день у автора не больше 24 ч (400); пишет событие WORK_LOGGED |
| ✅ | GET | `/worklogs?page=&size=` | кто видит задачу | `getWorkLogs` — новые по дате работы сверху |
| ✅ | PATCH | `/worklogs/{workLogId}` | автор или ADMIN проекта | `updateWorkLog` — любые из `minutes`, `workDate`, `comment` (пустой `comment` удаляет его) |
| ✅ | DELETE | `/worklogs/{workLogId}` | автор или ADMIN проекта | `deleteWorkLog` |

В архивном проекте списания только читаются (409 на изменения).

**Эффективная роль в проекте:** OWNER / ADMIN организации = ADMIN в любом проекте; иначе роль участника проекта; участник организации вне проекта — 403.

| Действие | USER | EXTENDED | ADMIN |
|---|---|---|---|
| смотреть задачи | ✅ | ✅ | ✅ |
| создать Task / Story / Bug | ✅ | ✅ | ✅ |
| создать Epic | ❌ | ✅ | ✅ |
| редактировать | только свои | любые | любые |
| менять статус | свои или где исполнитель | любые | любые |
| назначать исполнителя | взять свободную / снять себя | любого из проекта | любого из проекта |
| привязать к Epic (в т.ч. при создании) | ❌ | ✅ | ✅ |
| удалить | только свои | только свои | любые |

**Workflow статусов:** TODO → IN_PROGRESS; IN_PROGRESS → TODO или IN_REVIEW; IN_REVIEW → IN_PROGRESS или DONE; DONE → IN_PROGRESS (переоткрыть). Остальные переходы — 409.

Архивный проект (`ARCHIVED`) — только чтение (409 на изменения).

## 7. Общее

| | Что | Зачем |
|---|---|---|
| ✅ | Пагинация: `PageResponse` (`content`, `page`, `size`, `totalElements`, `totalPages`), по умолчанию 20, максимум 100, порядок по дате создания | уже на всех списках; Issue подключить так же |
| 🟡 | Логирование / аудит через AOP (`@Around`) | анализ: кто, что, сколько длилось, успех/отказ |
| 🟡 | OpenAPI / Swagger UI (springdoc) | живая документация API, удобно показывать в портфолио |
| ⚪ | CORS | понадобится, когда появится фронтенд на другом домене |

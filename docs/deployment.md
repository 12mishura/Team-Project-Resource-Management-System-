# Деплой TaskTracker на AWS

Приложение работает на одной виртуальной машине EC2 в Docker Compose:

```text
Интернет ──HTTPS──▶ Caddy (контейнер web: сертификат Let's Encrypt, статика фронтенда)
                     └── /api/* ──▶ Spring Boot (контейнер backend, порт 8080 только внутри Docker)
                                     └── PostgreSQL 17 (контейнер db, данные в томе db-data)
```

Снаружи открыты только порты 80 и 443. Бэкенд и база в интернет не смотрят.

Файлы деплоя:

| Файл | Зачем |
|---|---|
| `docker-compose.prod.yml` | три сервиса: `db`, `backend`, `web` |
| `backend/Dockerfile` | сборка jar и запуск на Java 25 JRE от непривилегированного пользователя |
| `frontend/Dockerfile` | сборка фронтенда и образ Caddy со статикой |
| `deploy/Caddyfile` | HTTPS, прокси `/api`, кэширование, заголовки безопасности |
| `deploy/production.env.example` | шаблон `.env` с секретами |
| `deploy/backup.sh` | бэкап базы (локально и при желании в S3) |
| `deploy/update.sh` | бэкап, `git pull`, пересборка и перезапуск |

В продакшне включены лимит попыток входа и регистрации (20 входов в минуту, 10 регистраций в час с одного IP), health-check `/api/actuator/health` и ночная очистка просроченных refresh-токенов.

## 1. Сервер EC2

Проект AWS работает в одном регионе — **eu-north-1 (Stockholm)**. Все ресурсы создаются там.

1. Консоль AWS → **EC2** → проверьте регион справа вверху: *Europe (Stockholm)* → **Launch instance**.
2. **Name:** `tasktracker`.
3. **OS:** *Ubuntu Server 24.04 LTS*.
4. **Instance type:**
   - `t4g.small` (ARM/Graviton, 2 ГБ) — дешевле, все наши образы собираются под ARM, при этом в **Architecture** выберите *64-bit (Arm)*;
   - или `t3.small` (x86, 2 ГБ). `*.micro` с 1 ГБ не берите: Spring Boot и PostgreSQL вместе занимают около 450 МБ, сборка образов требует больше.
5. **Key pair:** *Create new key pair* → тип `ED25519`, формат `.pem` → сохраните файл, второй раз его не скачать.
6. **Network settings → Edit:** создать security group:
   - SSH (22) — *Source: My IP*;
   - HTTP (80) — *Anywhere*;
   - HTTPS (443) — *Anywhere*.
7. **Storage:** 20 GiB, `gp3`.
8. **Launch instance**.
9. **Elastic IP:** EC2 → *Elastic IPs* → *Allocate* → выберите адрес → *Associate* с инстансом `tasktracker`. Адрес не изменится после перезапуска машины.

> Публичный IPv4 у AWS платный (около 3.6 $/мес), он списывается с кредитов вместе с машиной. Поставьте Budget-алерт в *Billing and Cost Management → Budgets*.

## 2. Подключение по SSH

На Windows (PowerShell), один раз ограничьте доступ к ключу, иначе SSH откажется его использовать:

```powershell
icacls .\tasktracker.pem /inheritance:r /grant:r "$($env:USERNAME):(R)"
```

```powershell
ssh -i .\tasktracker.pem ubuntu@<ELASTIC_IP>
```

Можно и без ключа на компьютере: EC2 → инстанс → **Connect → EC2 Instance Connect**.

## 3. Подготовка сервера

Все команды дальше выполняются на сервере.

```bash
sudo apt update && sudo apt -y upgrade
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
```

Swap на 2 ГБ, чтобы сборка образов не упиралась в память:

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Выйдите (`exit`) и подключитесь снова, чтобы группа `docker` применилась. Проверка: `docker run --rm hello-world`.

## 4. Код и секреты

```bash
git clone https://github.com/12mishura/Team-Project-Resource-Management-System-.git TaskTracker
cd TaskTracker
git switch main
```

Если репозиторий приватный, войдите через `gh auth login` или добавьте [deploy key](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys) только на чтение.

```bash
cp deploy/production.env.example .env
openssl rand -base64 48   # для JWT_SECRET
openssl rand -hex 24      # для DB_PASSWORD
nano .env
```

`SITE_ADDRESS`:

- без домена — `<ELASTIC_IP>.sslip.io`, например `13.48.10.20.sslip.io`. HTTPS работает, ничего покупать не нужно;
- с доменом — сначала создайте у регистратора A-запись на Elastic IP, затем впишите домен, например `tasktracker.example.com`.

`.env` в git не попадает. Храните его копию в менеджере паролей: без `JWT_SECRET` все пользователи будут разлогинены, без `DB_PASSWORD` не подключиться к базе.

## 5. Запуск

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
curl -s https://<SITE_ADDRESS>/api/actuator/health
```

Первая сборка идёт 5–10 минут. Ожидаемый ответ health-check: `{"status":"UP",...}`. Caddy получает сертификат при первом запросе по HTTPS, для этого порт 80 и 443 должны быть открыты, а адрес — указывать на сервер.

Демо-данные в продакшн не загружайте. Если нужен демо-стенд, задайте свой пароль: `BASE_URL=https://<SITE_ADDRESS>/api SEED_PASSWORD='<свой пароль>' ./scripts/seed-dev-data.sh`.

## 6. Обновление

```bash
cd ~/TaskTracker
./deploy/update.sh
```

Скрипт делает бэкап, подтягивает `main`, пересобирает и перезапускает изменившиеся контейнеры. Миграции базы Flyway применяет сам при старте бэкенда.

## 7. Бэкапы

Ручной бэкап: `./deploy/backup.sh` → файл в `~/tasktracker-backups`, хранятся 14 дней.

Ежедневно в 03:00 UTC:

```bash
(crontab -l 2>/dev/null; echo "0 3 * * * $HOME/TaskTracker/deploy/backup.sh >> $HOME/tasktracker-backups/backup.log 2>&1") | crontab -
```

Копия вне сервера (рекомендуется): создайте S3-бакет в eu-north-1, дайте инстансу IAM-роль с правом `s3:PutObject` на этот бакет, установите AWS CLI на сервере и добавьте в cron `BACKUP_S3_BUCKET=<имя бакета>` перед командой.

Восстановление (база перезаписывается данными из файла):

```bash
gunzip -c ~/tasktracker-backups/tasktracker-<дата>.sql.gz \
  | docker compose -f docker-compose.prod.yml exec -T db sh -c 'psql -q -U "$POSTGRES_USER" -d tasktracker -v ON_ERROR_STOP=1'
docker compose -f docker-compose.prod.yml restart backend
```

## 8. Мониторинг и логи

- Бесплатная проверка доступности: [UptimeRobot](https://uptimerobot.com), HTTP(s)-монитор на `https://<SITE_ADDRESS>/api/actuator/health`, письмо при падении.
- Логи: `docker compose -f docker-compose.prod.yml logs -f backend` (или `web`, `db`).
- Ресурсы: `docker stats`.

## 9. Неполадки

| Симптом | Что проверить |
|---|---|
| Сайт не открывается | `docker compose -f docker-compose.prod.yml ps`, security group (80/443 открыты), правильный `SITE_ADDRESS` |
| Ошибка сертификата | DNS указывает на Elastic IP, порт 80 открыт; логи `web` |
| `The server is not reachable right now` | бэкенд перезапускается или упал: логи `backend` |
| `Too many attempts` | сработал лимит; подождите минуту (вход) или час (регистрация) |
| Контейнер `backend` перезапускается | нет `JWT_SECRET`/`DB_PASSWORD` в `.env` или нехватка памяти (`docker stats`, swap) |

## 10. Остановка и расходы

- *Stop* инстанса останавливает оплату машины, но диск (EBS) и Elastic IP продолжают тарифицироваться.
- Полное удаление: *Terminate* инстанса, *Release* Elastic IP, удалить S3-бакет с бэкапами.
- Бесплатный план AWS действует 6 месяцев или до исчерпания кредитов. Заранее решите: перейти на платный план или перенести сервер. Перенос — те же шаги на любом VPS с Ubuntu плюс восстановление базы из бэкапа.

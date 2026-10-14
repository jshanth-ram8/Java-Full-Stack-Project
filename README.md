# Emergency Ambulance Booking & Tracking System

Java Spring Boot backend (SQLite by default, MySQL optional) with an HTML/CSS/JS frontend served from the same app
(`src/main/resources/static`). One container, one URL, no CORS.

## First login

On the first start an administrator account is created from the `ADMIN_EMAIL` and `ADMIN_PASSWORD`
environment variables (defaults: `admin@ambulance.com` / `admin123`, so change them for any real deployment).

Log in as the admin and set things up from the dashboard:
1. Add hospitals.
2. Add drivers.
3. Add ambulances and assign a driver to each.

Patients register themselves from the Register page.

## Run locally (nothing to install except Java 17)

```bash
mvn spring-boot:run
```

Open http://localhost:8080. The app uses **SQLite** by default and creates a file called
`ambulance.db` next to where you run it. Delete that file to start fresh.

## Use MySQL instead

Set the `mysql` profile and your connection details:

```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL="jdbc:mysql://localhost:3306/ambulance_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
export DB_USER=root
export DB_PASSWORD=your-password
mvn spring-boot:run
```

## Run with Docker

SQLite (data lives inside the container and is lost when it is removed):

```bash
docker build -t ambulance-app .
docker run -p 8080:8080 ambulance-app
```

MySQL on your laptop (`host.docker.internal` is how a container reaches it):

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=mysql \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/ambulance_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true" \
  -e DB_USER=root -e DB_PASSWORD=root ambulance-app
```

## Deploy on Render

1. Push this folder to GitHub.
2. On Render: **New > Web Service**, pick the repo, set **Runtime = Docker**.
3. Environment variables:
   - `ADMIN_EMAIL` and `ADMIN_PASSWORD` (your admin login)
   - To keep data between restarts, use a hosted MySQL (Aiven or TiDB Cloud have free tiers) and also add
     `SPRING_PROFILES_ACTIVE=mysql`, `DB_URL` (`jdbc:mysql://<host>:<port>/<dbname>?useSSL=true`), `DB_USER`, `DB_PASSWORD`.
   - Without these, the app runs on SQLite, which Render's free plan wipes on every restart.
4. Deploy. Every push redeploys automatically.

Free Render services sleep when idle, so the first request after a while is slow.
`render.yaml` is included if you prefer Blueprints (it assumes MySQL).

## How it works

- Patient books: the first free ambulance that has a driver is assigned and marked busy.
- Driver moves the trip forward one step at a time:
  Booking confirmed, Ambulance dispatched, Patient picked up, Trip completed.
  Completing the trip makes the ambulance free again.
- Patient can cancel until the patient is picked up. The ambulance is released.
- Login uses a plain `HttpSession` cookie. Passwords are stored as BCrypt hashes.
  `AuthInterceptor` enforces roles: `/api/admin/**` is ADMIN, `/api/driver/**` is DRIVER, `/api/bookings/**` is USER.

## API

| Method | Path | Who |
|--------|------|-----|
| POST | /api/auth/register, /login, /logout | anyone |
| GET | /api/auth/me | logged in |
| GET | /api/hospitals | logged in |
| POST | /api/bookings | patient |
| GET | /api/bookings/my | patient |
| PUT | /api/bookings/{id}/cancel | patient |
| GET | /api/driver/me, /api/driver/bookings | driver |
| PUT | /api/driver/availability | driver |
| PUT | /api/driver/bookings/{id}/status | driver |
| GET | /api/admin/users, /ambulances, /bookings | admin |
| POST | /api/admin/drivers, /ambulances, /hospitals | admin |
| PUT | /api/admin/ambulances/{id}/driver | admin |
| DELETE | /api/admin/users/{id}, /ambulances/{id}, /hospitals/{id} | admin |

## Project layout

```
src/main/java/com/ambulance/
  config/      AppConfig, AuthInterceptor, CurrentUser, DataSeeder
  controller/  Auth, Hospital, Booking, Driver, Admin
  service/     BookingService (assignment and status rules)
  repository/  Spring Data JPA repositories
  model/       User, Hospital, Ambulance, Booking, Role, BookingStatus
  dto/         Request and response records
  exception/   ApiException, GlobalExceptionHandler
src/main/resources/
  application.properties
  static/      HTML pages, css/style.css, js/*.js
```

## Ideas to extend

Live GPS tracking with Leaflet + OpenStreetMap, estimated arrival time, SMS or email notifications.

# BgApp — Background audio monitoring

Android app (Kotlin + Compose) plus a small Node.js backend. The app runs a **foreground service** that listens to the microphone, detects voice activity, saves short **WAV** clips, and **uploads** them to your backend. The backend stores metadata and can **email** clips via SMTP.

---

## What happens (main flow)

1. User enters **destination email** and **upload URL**, then taps **Save**.
2. User taps **Start monitoring** and grants **microphone** (and **notifications** on Android 13+).
3. A **foreground notification** stays visible while listening.
4. When speech is detected and then silence, a clip is saved and **queued for upload**.
5. The backend receives the file, saves a **database row**, and tries to **send it by email** (if SMTP is configured).

---

## What you need installed

| Piece | Purpose |
|--------|---------|
| **Android Studio** (or SDK + JDK 17) | Build and run the app |
| **Node.js 18+** | Run the backend |
| **MySQL** (or MariaDB, e.g. XAMPP) | Backend database |
| **SMTP account** (optional) | Backend sends email; without it, uploads still work |

---

## Backend (`backend/`)

### 1. Install dependencies

```bash
cd backend
npm install
```

### 2. Configure environment

Copy the example file and edit it:

```bash
copy .env.example .env
```

Set at least:

- **MySQL:** `MYSQL_HOST`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_DATABASE`
- **SMTP (for email):** `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `MAIL_FROM`

Run `npm run check-env` to confirm `.env` is loaded.

### 3. Create database and tables

As MySQL **root**, apply the bootstrap script (creates user `bgapp` and database `bgapp` — adjust password to match `.env` if needed):

```bash
mysql -u root -p < sql/bootstrap-mysql.sql
```

Then create app tables:

```bash
npm run db:init
```

### 4. Start the server

```bash
npm start
```

Default URL: **http://localhost:3000**

**API (short list)**

- `GET /health` — alive check  
- `POST /device/email` — register device + default email (JSON)  
- `GET /device/email?device_id=...` — read stored email  
- `POST /recordings/upload` — multipart: `recipient_email`, `audio` (WAV), optional `device_id`

### 5. Tests (optional)

```bash
npm test
```

Some tests need extra env (see `backend/.env.example`): `RUN_INTEGRATION_TESTS`, `SMTP_TEST_TO`, etc.

---

## Android app (`app/`)

### Build / install

From the **BgApp** folder (project root):

```bash
./gradlew installDebug
```

On Windows:

```bash
gradlew.bat installDebug
```

### Connect to the backend

- **Debug** build may prefill a base URL in the app. The full upload path must end with  
  **`/recordings/upload`** (the app adds this if you only set the base URL from `BuildConfig`).
- **Android Emulator:** `localhost` on the device is the emulator itself. Use your PC’s address from the emulator, e.g.  
  **`http://10.0.2.2:3000/recordings/upload`**
- **Physical phone:** use your PC’s LAN IP, e.g. `http://192.168.1.x:3000/recordings/upload`, or `adb reverse tcp:3000 tcp:3000` and keep a URL that points to the reversed port.

Debug builds use **cleartext HTTP** only in the `debug` source set (see `app/src/debug/AndroidManifest.xml`). Release should use **HTTPS** in production.

### Permissions

- **Microphone** — required for capture  
- **Notifications** — recommended so the foreground service notification shows reliably  
- **Internet** — uploads  

---

## Project layout (short)

```
BgApp/
├── app/                 # Android application
├── backend/             # Express API + MySQL + email pipeline
└── README.md            # This file
```

---

## Troubleshooting

| Issue | What to check |
|--------|----------------|
| MySQL “access denied” | User/password in `.env` match `bootstrap-mysql.sql`; run `npm run check-env`; start MySQL (e.g. XAMPP). |
| App cannot reach backend | Emulator → `10.0.2.2`; phone → LAN IP or `adb reverse`. |
| Email never arrives | `SMTP_*` in `backend/.env`; provider allows SMTP from your network. |
| Upload retries forever | Server returns 5xx or network errors; fix server or URL. Bad URL or 4xx stops the worker after failure (clip may remain on device until you fix and rescan). |

---

Use this project responsibly, with clear consent from anyone affected, and in line with app store rules and local law.

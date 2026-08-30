# Docs, a lightweight collaborative document editor

A small full stack document editor built for the Ajaia AI Native Full Stack Developer
assignment. Angular 18 on the front, Spring Boot 3 on the back, shipped as one
deployable Docker image.

Create and edit documents with rich text formatting, import files, attach files,
share with other people at a chosen access level, and roll back through version
history.

---

## Contents

- [Reviewing this project](#reviewing-this-project)
- [What is supported](#what-is-supported)
- [Configuration and the database](#configuration-and-the-database)
- [Running it locally](#running-it-locally)
- [Tests](#tests)
- [API reference](#api-reference)
- [Security](#security)
- [Deploying](#deploying)
- [Project layout](#project-layout)
- [Troubleshooting](#troubleshooting)
- [Further reading](#further-reading)

---

## Reviewing this project

There is no hosted instance. The app runs locally with two commands and no
configuration, and the walkthrough video shows the whole flow end to end. The
Dockerfile and `render.yaml` are included and were verified against a real Postgres,
so it can be deployed at any point, but a live URL is not part of this submission.

The fastest way to see it running:

```bash
cd backend && mvn spring-boot:run
```

```bash
cd frontend && npm install && npm start
```

Then open http://localhost:4200 and sign in with any account below.

| Item | Value |
| --- | --- |
| Password | `demo123`, the same for every account |

| Email | Name | Set up as |
| --- | --- | --- |
| alice@ajaia.test | Alice Bennett | Owns "Q3 product plan", shared with Bob and Carol |
| bob@ajaia.test | Bob Carter | Editor on Alice's document, owns "Standup notes" |
| carol@ajaia.test | Carol Diaz | Viewer on Alice's document, best for read only mode |
| dan@ajaia.test | Dan Everett | No documents, best for demonstrating a fresh share |

There is no signup. The login page lists the four accounts and shows the password in
a tooltip beside the password field, so nothing has to be typed from memory. The seed
data already contains a shared document, so sharing can be seen on first login
without setting anything up.

---

## What is supported

| Area | Supported |
| --- | --- |
| Formatting | Bold, italic, underline, H1, H2, body text, bulleted list, numbered list |
| Editing | Undo and redo, Tab and Shift+Tab to nest list items, keyboard shortcuts with hints in the tooltips, live word and character count |
| Saving | Debounced autosave with a visible state, and a flush if you navigate away mid edit |
| Paste | Content from Word or the web is cleaned to the tags the editor supports, so what you see is what gets stored |
| Version history | Every save snapshotted, nearby edits grouped, any version previewed and restored, restores are append only |
| Import as a new document | `.txt`, `.md`, `.markdown`, `.docx`, up to 2 MB |
| Attachments | Any file type, up to 5 MB each, always served as a download |
| Sharing | Viewer (read only) and Editor (read and write), roles changeable and revocable |
| Persistence | H2 file database locally, Postgres when deployed |

**Who can do what**

| Action | Owner | Editor | Viewer |
| --- | --- | --- | --- |
| Open and read | yes | yes | yes |
| Edit content and title | yes | yes | no |
| Upload and delete attachments | yes | yes | no |
| Read version history | yes | yes | yes |
| Restore a version | yes | yes | no |
| Change or revoke sharing | yes | no | no |
| Delete the document | yes | no | no |

Every one of these rules is enforced on the server, in a single service, and covered
by tests. Someone with no access at all receives a 404 rather than a 403, so the API
does not reveal which documents exist.

---

## Configuration and the database

**There is no `.env` file to fill in for local development, and no database to
install.** Everything needed to run locally lives in
`backend/src/main/resources/application.yml`, and the database is an H2 file the app
creates for itself at `backend/data/docsapp.mv.db` on first start. Delete that file
for a clean slate.

| Where | Database | Configuration needed |
| --- | --- | --- |
| Local development | H2 file, created automatically | None |
| Tests | H2 in memory, created and dropped per run | None |
| Deployed | Postgres, which you create on your host | Four environment variables |

For a deployment, copy `.env.example` to `.env` and fill in the four values, or paste
the same values into your host's environment settings.

Spring Boot does not read `.env` files by itself, so that file is used in one of two
ways: passed to Docker with `docker run --env-file .env ...`, or treated as the list
of values to copy into a dashboard such as Render's Environment tab. `.env` is
gitignored so a real one never gets committed, `.env.example` is committed as the
template.

The four variables are `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. Nothing else is
required, and no secret is ever stored in the repository.

## Running it locally

You need **Java 17 or newer**, **Maven**, and **Node 20 or newer**. No database and
no configuration.

### 1. Start the backend

```bash
cd backend && mvn spring-boot:run
```

Serves http://localhost:8080 and creates an H2 database file under `backend/data/`.
The demo accounts and seed documents are created on first start only, so deleting
`backend/data/` gives a clean slate.

### 2. Start the frontend

```bash
cd frontend && npm install && npm start
```

Open http://localhost:4200. The dev server proxies `/api` to port 8080, configured in
`frontend/proxy.conf.json`.

### Running the whole thing as one container

This is exactly what the deployed instance runs, with the Angular build inside the
Spring Boot jar on a single port.

```bash
docker build -t ajaia-docs . && docker run -p 8080:8080 ajaia-docs
```

Do not add `SPRING_PROFILES_ACTIVE=prod` for a local container run. The prod profile
marks the session cookie `Secure`, so the browser will not send it back over plain
HTTP and sign in will appear to fail. Prod expects TLS in front of it.

---

## Tests

```bash
cd backend && mvn test
```

**42 backend tests.** Access rules end to end through the HTTP layer, the auth flow,
version history and its coalescing, file import, HTML sanitizing, login throttling,
and the API error contract.

```bash
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless
```

**69 frontend tests.** The paste sanitizer, the API service contract in both
directions, and the login, editor, share panel and confirm dialog components,
including their failure paths.

Both suites cover refusals as well as working paths. Wrong passwords, forbidden
edits, invalid uploads, unreachable servers and malformed requests all have tests.

---

## API reference

All endpoints are under `/api` and require a session except where noted. Failures
return `{ "status": number, "message": string, "fieldErrors"?: object }`.

| Method and path | Purpose |
| --- | --- |
| `POST /api/auth/login` | Sign in. Public. |
| `POST /api/auth/logout` | Sign out and invalidate the session. |
| `GET /api/auth/me` | The signed in user. |
| `GET /api/auth/demo-users` | Seeded accounts for the login page. Public, demo only. |
| `GET /api/documents` | Owned and shared lists together. |
| `POST /api/documents` | Create an empty document. |
| `GET /api/documents/{id}` | Open a document. |
| `PATCH /api/documents/{id}` | Update the title, the content, or both. |
| `DELETE /api/documents/{id}` | Delete. Owner only. |
| `POST /api/documents/import` | Upload a file as a new document. |
| `GET POST /api/documents/{id}/shares` | List or grant access. Granting is owner only. |
| `DELETE /api/documents/{id}/shares/{userId}` | Revoke access. Owner only. |
| `GET POST /api/documents/{id}/attachments` | List or upload attachments. |
| `GET DELETE /api/documents/{id}/attachments/{attachmentId}` | Download or remove one. |
| `GET /api/documents/{id}/versions` | Version history. |
| `GET /api/documents/{id}/versions/{versionId}` | One version with its content. |
| `POST /api/documents/{id}/versions/{versionId}/restore` | Restore. Needs edit access. |

**Status codes used**

| Code | When |
| --- | --- |
| 400 | Validation failure, unreadable body, bad path value, unsupported file type |
| 401 | Not signed in, or wrong credentials |
| 403 | Signed in but not allowed, for example a viewer trying to save |
| 404 | Does not exist, or the caller has no access to it at all |
| 405 | Wrong HTTP method |
| 409 | A conflicting concurrent change |
| 413 | Upload too large |
| 415 | Wrong content type |
| 429 | Too many failed sign in attempts |

---

## Security

| Concern | Approach |
| --- | --- |
| Password storage | BCrypt |
| Session | httpOnly, SameSite=Lax cookie, marked Secure under the prod profile |
| Session fixation | Session id rotated on login |
| CSRF | Spring writes `XSRF-TOKEN`, Angular returns it in `X-XSRF-TOKEN` |
| User enumeration | A wrong password and an unknown email return the identical 401 |
| Brute force | Five failed attempts locks that account for fifteen minutes |
| Stored XSS | All content sanitized server side with a strict jsoup allowlist |
| Reflected XSS | Content Security Policy with no inline script and no off site sources |
| File upload XSS | Attachments always served `Content-Disposition: attachment` |
| Clickjacking | `X-Frame-Options: DENY` and `frame-ancestors 'none'` |
| Transport | HSTS for one year including subdomains |
| Information leaks | No stack traces in responses, 404 instead of 403 for no access |

Sessions are used rather than JWTs on purpose. A JWT has to live somewhere JavaScript
can read it, and it cannot be revoked without rebuilding server side session state.
The reasoning is set out in full in `docs/ARCHITECTURE.md`.

`GET /api/auth/demo-users` is a deliberate exception. It lists the seeded accounts to
an anonymous caller so reviewers do not have to guess email addresses. It is a user
enumeration hole, it is labelled as such in the code, and it disappears along with the
seeded accounts.

---

## Deploying

The Dockerfile produces a single self contained image, so any host that runs a
container will work. These steps are for Railway with a Supabase database, which is
the combination this project was deployed with. Render, Koyeb and Fly all work the
same way, only the dashboard differs.

### 1. Create the database on Supabase

Create a project, choose a region, and save the database password it generates.

Open **Connect** in the project and pick the **Session pooler** string. This matters:
the direct connection is IPv6 only on the free plan, and the transaction pooler on
port 6543 does not support the prepared statements the JDBC driver uses. The session
pooler on port 5432 is IPv4 and supports both.

It looks like this, and the three parts map onto the three variables below:

```
postgresql://postgres.abcdefghijklm:YOUR-PASSWORD@aws-0-eu-central-1.pooler.supabase.com:5432/postgres
             └── username ────────┘ └ password ─┘ └────────── host ──────────────────┘ port  database
```

### 2. Create the service on Railway

**New Project**, then **Deploy from GitHub repo**, and pick this repository. Railway
reads  and builds the root Dockerfile. No build or start command is
needed.

**Leave the root directory empty.** It is tempting to point Railway at ,
but the Dockerfile at the repository root is what builds the Angular app and copies
it into the Spring Boot jar. Pointing at a subfolder skips that and you end up with
an API and no user interface. This is one service, not two.

Under **Settings**, generate a public domain. Railway injects `PORT` itself and the
app reads it, so no port configuration is required.

### 3. Set the variables

In the service's **Variables** tab:

| Name | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://aws-0-YOUR-REGION.pooler.supabase.com:5432/postgres?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `postgres.YOUR-PROJECT-REF` |
| `SPRING_DATASOURCE_PASSWORD` | your Supabase database password |

Note the `jdbc:` prefix and the `?sslmode=require` suffix. Supabase gives you a
`postgresql://` URL with the credentials inline, which is not the JDBC form, so it
has to be split apart as shown above.

### 4. Check it

The first build takes a few minutes. When it is done, open the domain. If it loads
the login page and you can sign in as `alice@ajaia.test` with `demo123`, the database
connection is working, because the seeded accounts are written on first start.

**Test the connection string before deploying** if you would rather not debug in a
dashboard. This runs the real image locally against Supabase:

```bash
docker build -t ajaia-docs . && docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://HOST:5432/postgres?sslmode=require" \
  -e SPRING_DATASOURCE_USERNAME="postgres.YOUR-PROJECT-REF" \
  -e SPRING_DATASOURCE_PASSWORD="YOUR-PASSWORD" \
  ajaia-docs
```

Leave `SPRING_PROFILES_ACTIVE` out of that local command. The prod profile marks the
session cookie Secure, so a browser will not send it back over plain HTTP. The
database connection is still exercised, which is the part being tested.

### Notes on the free tiers

- A Supabase project on the free plan **pauses after a week of inactivity**. If this
  is a link someone might open weeks later, open the Supabase dashboard first to wake
  it, or use Railway's own Postgres instead, which does not pause.
- Without `SPRING_PROFILES_ACTIVE=prod` the app falls back to an H2 file inside the
  container. That works and reseeds itself, but anything written is lost on redeploy.
  It is a reasonable choice for a pure demo and needs no database at all.
- The container has been measured starting in 46 seconds inside 512 MB of memory and
  half a CPU, settling at around 225 MB. It fits comfortably in a small free instance.

**Verified before handover:** a fresh `git clone` builds the image with no extra
steps, the container honours an injected `PORT`, serves the app and its deep links,
protects the API, and the prod profile has been run against a real Postgres instance
including an attachment upload, download and delete.

---

## Project layout

```
backend/       Spring Boot 3.3, Java 17, Maven
  domain/      entities and the access level enum
  repo/        Spring Data repositories and list projections
  service/     access rules, documents, sharing, versions, import, sanitizing
  web/         controllers, DTO records, one exception handler
frontend/      Angular 18, standalone components
  core/        services, guard, error mapping, paste sanitizer, shared dialog
  features/    login, dashboard, editor with share, files and history panels
docs/          architecture note, AI workflow note, video script, links
Dockerfile     builds the Angular app into the Spring Boot jar
render.yaml    optional deployment blueprint
```

---

## Troubleshooting

**Sign in does nothing, or writes fail with 403.** The CSRF cookie is missing. Reload
the page once, which fetches a fresh token.

**Sign in fails in a local container.** You probably set `SPRING_PROFILES_ACTIVE=prod`,
which marks the session cookie Secure so the browser will not return it over HTTP.
Drop the profile for local runs.

**"Too many failed sign in attempts".** Five wrong passwords locks that account for
fifteen minutes. Use one of the other demo accounts, or restart the backend, since
the counters are held in memory.

**Port 8080 already in use.** Run with `SERVER_PORT=8081` and update
`frontend/proxy.conf.json` to match.

**The editor is blank after an import.** The file type is supported but produced no
text. Word files that are only images or tables import as empty, which is expected.

**Styles look unstyled in a container build.** Should not happen now, but if it does,
check the browser console for a Content Security Policy error. Angular's critical CSS
inlining emits an inline `onload` handler that the policy blocks, which is why
`inlineCritical` is turned off in `angular.json`.

---

## Further reading

- `docs/ARCHITECTURE.md` covers what was built, how it fits together, and why each
  significant call was made, including the bugs found while auditing it.
- `docs/AI-WORKFLOW.md` covers how AI tooling was used, what was rejected, and how
  correctness was verified.
- `SUBMISSION.md` lists everything in this folder, what works, what is incomplete,
  and what would come next.

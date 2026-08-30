# Docs, a lightweight collaborative document editor

A small full stack document editor built for the Ajaia AI Native Full Stack assignment.
Angular 18 on the front, Spring Boot 3 on the back, one deployable artifact.

You can create and rename documents, edit them with basic rich text formatting,
import a file as a new document, attach files to a document, and share a document
with another user as a viewer or an editor.

## Live deployment

| Item | Value |
| --- | --- |
| URL | see `docs/deployment-url.txt` |
| Accounts | alice@ajaia.test, bob@ajaia.test, carol@ajaia.test, dan@ajaia.test |
| Password | `demo123` for every account |

The login page lists the four accounts and shows the password in a tooltip next to
the password field, so nothing needs to be typed from memory.

The seed data already contains a shared document, so the sharing behaviour can be
seen on first login without setting anything up. Alice owns "Q3 product plan" and has
shared it with Bob as an editor and Carol as a viewer.

## What is supported

| Area | Supported |
| --- | --- |
| Formatting | Bold, italic, underline, H1, H2, body text, bulleted list, numbered list |
| Editing | Undo and redo, Tab and Shift+Tab to nest list items, keyboard shortcuts, live word and character count, autosave with a visible state |
| Paste | Content from Word or the web is cleaned to the tags the editor supports, so what you see is what gets stored |
| Version history | Every save is snapshotted, nearby edits grouped, any version can be previewed and restored |
| Import as new document | `.txt`, `.md`, `.markdown`, `.docx`, up to 2 MB |
| Attachments | Any file type, up to 5 MB each |
| Sharing roles | Viewer (read only) and Editor (read and write) |
| Persistence | H2 file database locally, Postgres when deployed |

Only the owner can delete a document or change who it is shared with. Restoring a
version needs edit access. These limits are also stated in the user interface next
to the controls they apply to.

## Running it locally

You need Java 17 or newer, Maven, and Node 20 or newer.

### 1. Start the backend

```bash
cd backend && mvn spring-boot:run
```

It listens on http://localhost:8080 and creates an H2 database file under
`backend/data/`. The four demo accounts and the seed documents are created on the
first start only, so deleting `backend/data/` gives you a clean slate.

### 2. Start the frontend

```bash
cd frontend && npm install && npm start
```

Open http://localhost:4200. The dev server proxies `/api` to port 8080, which is
configured in `frontend/proxy.conf.json`.

### Running everything as one jar

This is what the deployed instance runs. It builds the Angular app into the Spring
Boot static folder and serves both from port 8080.

```bash
docker build -t ajaia-docs . && docker run -p 8080:8080 ajaia-docs
```

## Tests

Backend, 42 tests covering the access rules, the auth flow, version history, file
import, HTML sanitizing, login throttling and the API error contract:

```bash
cd backend && mvn test
```

Frontend, 69 tests covering the paste sanitizer, the API service contract, and the
login, editor, share panel and dialog components, including their failure paths:

```bash
cd frontend && npm test -- --watch=false --browsers=ChromeHeadless
```

## Deploying

The Dockerfile produces a single image, so any host that runs a container works.
These are the steps for Render, which has a free tier and needs no card.

1. Push this folder to a GitHub repository.
2. Create a free Postgres database. Render offers one, and Neon also works. Copy
   the JDBC url, the username and the password.
3. In Render, create a **New Web Service**, point it at the repository, and pick
   **Docker** as the runtime. Leave the Dockerfile path as `./Dockerfile`.
4. Add these environment variables:

   | Name | Value |
   | --- | --- |
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/DBNAME` |
   | `SPRING_DATASOURCE_USERNAME` | your database user |
   | `SPRING_DATASOURCE_PASSWORD` | your database password |

5. Deploy. The first request wakes the free instance and can take around a minute.

`render.yaml` in the repository root holds the same settings if you prefer to
create the service from a blueprint. Render supplies `PORT` on its own and the app
reads it.

Without `SPRING_PROFILES_ACTIVE=prod` the app falls back to the local H2 file, which
does not survive a redeploy on a free instance. That is the only reason Postgres is
needed for the hosted build.

## Project layout

```
backend/     Spring Boot 3.3, Java 17, Maven
frontend/    Angular 18, standalone components
docs/        architecture note, AI workflow note, video script and links
Dockerfile   builds the Angular app into the Spring Boot jar
render.yaml  optional deployment blueprint
```

## Further reading

- `docs/ARCHITECTURE.md` covers what was built, how it fits together, and why each
  significant call was made.
- `docs/AI-WORKFLOW.md` covers how AI tooling was used and what was rejected.
- `SUBMISSION.md` lists everything in this folder and states what is incomplete.

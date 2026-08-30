# Submission

Ajaia AI Native Full Stack Developer assignment. A lightweight collaborative
document editor, Angular 18 and Spring Boot 3.

## What is in this folder

| Path | What it is |
| --- | --- |
| `README.md` | Setup, run instructions, supported file types, deployment steps |
| `SUBMISSION.md` | This file |
| `docs/ARCHITECTURE.md` | What was built, how it fits together, why each call was made |
| `docs/AI-WORKFLOW.md` | Which AI tools were used, what was rejected, how correctness was verified |
| `docs/VIDEO-SCRIPT.md` | Script for the walkthrough recording |
| `docs/walkthrough-video-url.txt` | Link to the walkthrough video |
| `docs/deployment-url.txt` | Link to the live deployment |
| `backend/` | Spring Boot 3.3 application, Java 17, Maven |
| `frontend/` | Angular 18 application, standalone components |
| `Dockerfile` | Builds the Angular app into the Spring Boot jar, one image |
| `render.yaml` | Optional Render blueprint |
| `.gitattributes` | Normalises line endings so Linux builds are unaffected by Windows checkouts |

## Handover state

| Check | Result |
| --- | --- |
| Git | One commit on `main`, 124 tracked files, no generated output committed |
| Secrets | None in the repository, all deployment values come from environment variables |
| Fresh clone | Clones and builds the Docker image with no extra steps |
| Container | Honours an injected `PORT`, serves the app, its deep links and the API |
| Postgres | Prod profile run against a real instance, including attachment upload, download and delete |
| Tests | 42 backend and 69 frontend, all passing |

Still to do, because they need your accounts: push to GitHub, create the Render
service and Postgres database, record the walkthrough video, and upload the folder to
Drive. Steps for each are in `README.md` and `docs/VIDEO-SCRIPT.md`.

## Test accounts

| Email | Name | Set up as |
| --- | --- | --- |
| alice@ajaia.test | Alice Bennett | Owns "Q3 product plan", shared it with Bob and Carol |
| bob@ajaia.test | Bob Carter | Editor on Alice's document, owns "Standup notes" |
| carol@ajaia.test | Carol Diaz | Viewer on Alice's document, good for showing read only mode |
| dan@ajaia.test | Dan Everett | No documents, good for showing a fresh share |

Password for all four accounts: `demo123`

The login page lists the accounts and shows the password in a tooltip, so nothing has
to be typed from memory.

## Suggested review path

1. Sign in as **alice@ajaia.test**.
2. Open "Q3 product plan". Type into it and watch the header change to
   All changes saved. Try the toolbar. Rename the document in the header. Press Tab
   inside a list item to nest it. Watch the word count at the bottom.
3. Paste something formatted from a web page or a Word document. The colours and fonts
   are dropped on the way in, on purpose, because that is what the server will store.
   Reload and confirm the text is exactly what you saw.
4. Reload the page. The content and the name are still there.
5. Open the **History** tab. Your edits appear as a version. Click one to preview it,
   then restore it. The version you rolled back from is still in the list, so the
   restore can itself be undone.
6. Open the **Share** tab. Share the document with **dan@ajaia.test** as a viewer.
   Attach a file in the **Files** tab.
7. Go back and use **Import a file** with any `.md` or `.txt` file. It opens as a new
   document with its formatting preserved.
8. Sign out. Sign in as **carol@ajaia.test**. The document appears under Shared with
   me with a View only badge, the toolbar is turned off inside it, and the History tab
   is readable but offers no restore button.

## Status

### Working end to end

**Documents and editing**
- Create, rename, edit, save and reopen documents
- Bold, italic, underline, H1, H2, body text, bulleted list, numbered list
- Undo and redo, keyboard shortcuts, shortcut hints in the button tooltips
- Tab and Shift+Tab nest and unnest list items
- Live word and character count
- Autosave with a visible saving and saved state, and a flush if you navigate away
  mid edit
- Paste from Word or the web is cleaned to the tags the editor supports, so the text
  on screen matches what is stored

**Version history**
- Every save is snapshotted, with edits made close together by the same person grouped
  into one version
- Any version can be previewed in place and restored
- History is append only, so restoring never destroys the versions it rolled past, and
  a restore is itself undoable
- Restored versions are badged with what they were restored from
- Reading history needs read access, restoring needs edit access

**Files**
- Import `.txt`, `.md` and `.docx` as a new document, up to 2 MB
- Attach and download files on a document, any type, up to 5 MB each

**Sharing and access**
- Share with a viewer or editor role, change a role, remove access
- Owned and shared documents shown separately, shared ones badged with owner and role
- Read only mode for viewers, with the toolbar disabled and the reason stated
- Sign in and sign out with hashed passwords and a session cookie

**Engineering**
- Persistence across refresh and restart
- Validation and error handling on every endpoint, with readable messages
- Session cookie auth with BCrypt, CSRF tokens, login throttling, and a full set of
  security headers including a content security policy
- Consistent JSON errors on every failure path, with correct status codes rather
  than a blanket 500
- List endpoints that do not load document bodies, and a query count that does not
  grow with the number of documents
- 42 backend tests and 69 frontend tests, all passing, covering both the working
  paths and the refusals

### Not built, on purpose

- Real time collaborative editing. Correct multi cursor editing needs operational
  transform or CRDTs plus a websocket layer and a structured document model, which
  does not fit the timebox. A version that silently loses writes would be worse than
  not having it.
- Signup and password reset. Seeded accounts serve the review better.
- Comments, suggestion mode, folders, search, export to PDF.

### Known limits

- Two people editing the same document at once will overwrite each other. Last write
  wins. There is no conflict detection yet. Version history softens this, because the
  overwritten text is still recoverable, but it does not prevent it.
- History shows each version whole. There is no diff between two versions.
- Versions accumulate without limit. A real product would thin old ones out.
- Attachment bytes are stored in the database row. Fine at a 5 MB cap, wrong above it.
- The schema is created by Hibernate `ddl-auto: update`. Right for a demo, wrong for
  anything holding real data.
- The free deployment tier sleeps when idle, so the first request can take about a
  minute.

### With another two to four hours

1. A diff view between two versions. History is currently a rollback button, and a
   diff is what turns it into a record.
2. Optimistic concurrency, a version column and a 409 on mismatch, so a conflict is
   surfaced instead of silently dropping someone's work.
3. Presence indicators showing who else has the document open, labelled honestly as
   presence rather than co editing.
4. Attachment storage moved behind an interface, filesystem locally and S3 in
   production, so the size cap can rise.
5. Flyway migrations in place of `ddl-auto`, and a retention rule for old versions.
6. Move the login throttle counters out of memory, so the limit holds across more
   than one instance.

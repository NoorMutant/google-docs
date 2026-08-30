# Submission

**Repository: https://github.com/NoorMutant/google-docs**

Ajaia AI Native Full Stack Developer assignment, submitted by Noor Muhammad.

A lightweight collaborative document editor, built with Angular 18 and Spring Boot 3
and shipped as a single Docker image.

---

## The short version

Google Docs is roughly a decade of engineering. The useful question inside a short
timebox is not how much of it to imitate, but which slice carries the most signal.

Rather than cover ten features shallowly, I built the required scope properly and
then went deep on two things: **the editing experience** and **version history**,
with a real sharing and permission model enforced underneath both.

Every access rule is decided in one service on the server, not by hiding buttons in
the browser, and there are 111 automated tests covering the refusals as much as the
working paths.

---

## Trying it

| | |
| --- | --- |
| **Repository** | https://github.com/NoorMutant/google-docs |
| **Live URL** | see `docs/deployment-url.txt` |
| **Walkthrough video** | see `docs/walkthrough-video-url.txt` |
| **Password** | `demo123`, the same for every seeded account |

Running it locally takes two commands and needs **no configuration and no database
to install**. An H2 file database is created automatically on first start.

```bash
cd backend && mvn spring-boot:run
```

```bash
cd frontend && npm install && npm start
```

Then open http://localhost:4200. Requires Java 17+, Maven and Node 20+.

### Seeded accounts

There is no signup. Four accounts are created on first start, and the login page
lists them with the password in a tooltip, so nothing has to be typed from memory.

| Email | Set up as |
| --- | --- |
| alice@ajaia.test | Owns "Q3 product plan", shared with Bob and Carol |
| bob@ajaia.test | Editor on Alice's document, owns "Standup notes" |
| carol@ajaia.test | Viewer on Alice's document, best for showing read only mode |
| dan@ajaia.test | No documents, best for demonstrating a fresh share |

The seed data already contains a shared document, so the sharing behaviour is visible
on first login without setting anything up.

### A ten minute review path

1. Sign in as **alice@ajaia.test**. Open "Q3 product plan", type into it, and watch
   the header change to All changes saved. Try the toolbar, rename the document,
   press Tab inside a list item to nest it, watch the live word count.
2. **Paste something formatted** from a web page or a Word document. The colours and
   fonts are dropped deliberately, because that is exactly what the server stores.
   Reload and confirm the text is precisely what you saw.
3. Reload the page. Content and title are still there.
4. Open the **History** tab. Click a version to preview it, then restore it. The
   version you rolled back from is still listed, so the restore is itself undoable.
5. Open the **Share** tab, share with **dan@ajaia.test** as a viewer. Attach a file
   in the **Files** tab.
6. Back on the dashboard, **Import a file** with any `.md` or `.txt`. It opens as a
   new document with its structure preserved.
7. Sign out, sign in as **carol@ajaia.test**. The document appears under Shared with
   me, badged View only, the toolbar is disabled with the reason stated, and the
   History tab is readable but offers no restore button.

---

## What is in the repository

| Path | What it is |
| --- | --- |
| `README.md` | Setup, configuration, API reference, security model, deployment, troubleshooting |
| `SUBMISSION.md` | This file |
| `docs/ARCHITECTURE.md` | What was built, how it fits together, and why each significant call was made |
| `docs/AI-WORKFLOW.md` | Which AI tools were used, what was rejected, how correctness was verified |
| `docs/VIDEO-SCRIPT.md` | Script for the walkthrough recording |
| `docs/deployment-url.txt` | Live link and deployment state |
| `docs/walkthrough-video-url.txt` | Link to the walkthrough video |
| `backend/` | Spring Boot 3.3, Java 17, Maven |
| `frontend/` | Angular 18, standalone components |
| `Dockerfile` | Builds the Angular app into the Spring Boot jar as one image |
| `railway.json` | Pins Railway to the root Dockerfile and the health check path |
| `render.yaml` | Equivalent blueprint for Render |
| `.env.example` | Template for the four deployment variables. None are needed locally |
| `.gitattributes` | Normalises line endings so Windows checkouts do not affect Linux builds |

---

## Against the brief

| Requirement | Status |
| --- | --- |
| Create a document | Yes, blank or by importing a file |
| Rename a document | Yes, inline in the header, saved on blur and on Enter |
| Edit in a browser | Yes, with debounced autosave and a visible save state |
| Save and reopen | Yes, persisted and verified across refresh and restart |
| Bold, italic, underline | Yes, with keyboard shortcuts |
| Headings or size variation | Yes, H1, H2 and body text |
| Bulleted or numbered lists | Yes, with Tab and Shift+Tab nesting |
| File upload | Two paths: import as a new document, and attachments on a document |
| Stated file type limits | Yes, in the interface and in the README |
| A document owner | Yes |
| Grant another user access | Yes, as viewer or editor, changeable and revocable |
| Owned versus shared distinction | Yes, separate lists, badged with owner and role |
| Documents survive refresh | Yes |
| Formatting preserved | Yes, sanitized HTML, matched between client and server |
| Shared access demonstrable | Yes, seeded so it is visible on first login |
| Setup and run instructions | Yes, two commands, no configuration |
| Working deployment | Docker image, deployed to Railway with a Supabase Postgres |
| Validation and error handling | Yes, on every endpoint, with a consistent JSON error shape |
| At least one meaningful test | 111 tests, 42 backend and 69 frontend |
| Architecture note | `docs/ARCHITECTURE.md` |
| AI workflow note | `docs/AI-WORKFLOW.md` |
| Walkthrough video | See `docs/walkthrough-video-url.txt` |
| Optional stretch | Version history, and role based sharing permissions |

---

## What was built

### The editor

Bold, italic and underline with keyboard shortcuts, H1, H2 and body text, bulleted
and numbered lists, undo and redo, and Tab and Shift+Tab to nest and unnest list
items. A live word and character count. Debounced autosave with a visible state, and
a flush if you navigate away mid edit.

**Paste is cleaned in the browser to exactly the tag set the server stores.** Without
that, pasting from Word shows colours and fonts that vanish silently on the next
reload once the server has sanitized the HTML. Nothing errors, the document simply
changes behind the user's back. This was found by using the app rather than reading
the code, and it is written up in the AI workflow note.

### Version history

Every save is snapshotted. Autosave fires on every pause in typing, so snapshotting
each one would bury the history under hundreds of near identical rows. Consecutive
saves by the same person inside a two minute window update one entry instead, the
same idea as the way Google Docs groups edits into sessions.

**History is append only.** Restoring version 3 does not delete versions 4 and 5. It
writes version 3's content forward as the newest version, tagged with what it was
restored from, so a restore can itself be undone. That matters because restore is the
one action in the app that can destroy someone else's work.

Reading history needs read access. Restoring needs edit access.

### Sharing and access control

| Action | Owner | Editor | Viewer |
| --- | --- | --- | --- |
| Open and read | yes | yes | yes |
| Edit content and title | yes | yes | no |
| Upload and delete attachments | yes | yes | no |
| Read version history | yes | yes | yes |
| Restore a version | yes | yes | no |
| Change or revoke sharing | yes | no | no |
| Delete the document | yes | no | no |

One service, `DocumentAccessService`, answers what a user may do with a document.
Controllers never compare owner ids themselves, because access checks copy pasted
into each endpoint are how the fifth endpoint ends up with a subtly different rule,
and the fifth endpoint is the one that leaks.

Someone with no access gets **404, not 403**. A 403 would confirm the document
exists, which turns a list of ids into a way to probe what other people are working
on.

### File handling

**Import** turns a `.txt`, `.md` or `.docx` file into a new document you own, up to
2 MB. Markdown goes through commonmark, Word through Apache POI with heading styles,
lists and bold or italic runs mapped across. Rejected types name the extension that
was sent rather than failing generically.

**Attachments** hang off an existing document, any type, 5 MB each. They are always
served with `Content-Disposition: attachment`, never inline, because rendering user
supplied files on your own origin is a stored cross site scripting hole.

### Security

| Concern | Approach |
| --- | --- |
| Password storage | BCrypt |
| Session | httpOnly, SameSite=Lax cookie, Secure in production |
| Session fixation | Session id rotated on login |
| CSRF | Spring writes `XSRF-TOKEN`, Angular returns it in `X-XSRF-TOKEN` |
| User enumeration | A wrong password and an unknown email return the identical 401 |
| Brute force | Five failed attempts locks that account for fifteen minutes |
| Stored XSS | Server side sanitizing with a strict jsoup allowlist |
| Reflected XSS | Content Security Policy, no inline script, no off site sources |
| Clickjacking | `X-Frame-Options: DENY` and `frame-ancestors 'none'` |
| Transport | HSTS for one year including subdomains |
| Information leaks | No stack traces in responses, 404 rather than 403 for no access |

Sessions rather than JWTs, deliberately. A JWT has to live where JavaScript can read
it, and it cannot be revoked without rebuilding the server side state that makes it a
session again. The full reasoning is in the architecture note.

---

## Engineering

**One deployable artifact.** The Angular build is copied into the Spring Boot jar, so
the whole product is one image on one port with one URL. No CORS setup, no second
deploy to keep in sync, and the session cookie stays `SameSite=Lax`.

**Errors have one shape.** Every failure leaves through a single handler as
`{ status, message, fieldErrors? }`, with the correct status code. A malformed body,
a bad path value, the wrong content type and an invalid enum value are 400, 400, 415
and 400, not a blanket 500.

**Lists do not load what they do not show.** Both document lists use projections that
select exactly the columns rendered, so the query count does not grow with the number
of documents and no document body is fetched to draw a list of titles.

**111 tests, all passing.**

- **42 backend.** Access rules end to end through the HTTP layer, the auth flow,
  version history and its coalescing, file import, HTML sanitizing, login throttling,
  and the API error contract.
- **69 frontend.** The paste sanitizer, the API service contract in both directions,
  and the login, editor, share panel and dialog components.

Coverage was aimed at what is genuinely risky rather than spread evenly. The largest
single group of tests sits on the paste path, because that is the bug that got past
everything else.

---

## Status

### Not built, on purpose

- **Real time collaborative editing.** Correct multi cursor editing needs operational
  transform or CRDTs, a websocket layer, and a structured document model instead of
  the HTML this stores. It does not fit the timebox, and a version that silently
  loses writes is worse than not having it.
- **Signup and password reset.** Seeded accounts serve a review better.
- **Comments, suggestion mode, folders, search, export to PDF.**

### Known limits

- Two people editing the same document at once overwrite each other, last write wins.
  Version history softens this because the overwritten text stays recoverable, but it
  does not prevent the clash.
- History shows each version whole. There is no diff between two versions.
- Versions accumulate without limit. A real product would thin old ones out.
- Attachment bytes are stored in the database row. Fine at a 5 MB cap, wrong above.
- The schema is created by Hibernate `ddl-auto: update`, which is right for a demo
  and wrong for anything holding real data.
- Login throttle counters are held in memory, so they protect a single instance.

### With another two to four hours

1. A **diff view** between two versions. History is currently a rollback button, and
   a diff is what turns it into a record.
2. **Optimistic concurrency**, a version column and a 409 on mismatch, so a conflict
   is surfaced rather than silently dropping someone's work.
3. **Presence indicators** showing who else has the document open, labelled honestly
   as presence rather than co editing.
4. **Attachment storage behind an interface**, filesystem locally and S3 in
   production, so the size cap can rise.
5. **Flyway migrations** in place of `ddl-auto`, and a retention rule for versions.

---

## On AI usage

Claude Code wrote a large share of the code volume and, more usefully, drove a real
browser through every flow to verify it. It made no product decisions: what to build,
what to cut, and how to spend the time were settled before any code was generated.

Its most expensive suggestions were wrong in ways that only showed up by running the
software:

- A CSRF filter registered outside the security chain, which would have broken every
  write request.
- The editor reading its content at the wrong point in the Angular lifecycle, so
  every document rendered blank.
- A version history design writing one snapshot per autosave, which makes the feature
  useless within a minute of typing.
- A content security policy that silently broke the production stylesheet, visible
  only by building the image and opening it.

Two tests written during the audit failed, and both times the code was wrong rather
than the test. `docs/AI-WORKFLOW.md` has the full account.

The review step is not optional overhead. It is where the value gets captured.

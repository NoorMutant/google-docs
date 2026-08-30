# Architecture note

What was built, how it fits together, and why each call was made.

## The shape of the problem

The brief asks for a Google Docs style editor with creation, editing, upload,
sharing and persistence, inside a short timebox. Google Docs is roughly a decade of
work, so the only useful question is which slice carries the most signal.

The slice chosen here is **a document that one person owns and another person can be
given a specific level of access to**. Everything else was arranged around that.
Editing had to be good enough to feel like a product, upload had to be genuinely
useful rather than a demo button, and sharing had to have real rules that are
enforced on the server, not hidden buttons on the client.

## Stack

| Layer | Choice | Reason |
| --- | --- | --- |
| Frontend | Angular 18, standalone components, signals | Requested. Standalone components remove the module boilerplate, so the code is closer to what the app actually does. |
| Backend | Spring Boot 3.3, Java 17 | Requested. Bean validation, Spring Security and Spring Data all come from the same framework, which keeps the wiring small. |
| Database | H2 file locally, Postgres in production | H2 in file mode means a reviewer can clone and run with nothing installed. The same JPA code runs on Postgres by switching a profile. |
| Packaging | One Docker image | The Angular build is copied into the Spring Boot static folder. One artifact, one port, one URL for reviewers. No CORS setup and no second deploy to keep in sync. |

## How the pieces fit

```
Angular app
  core/            auth service, documents service, route guard, error mapping,
                   paste sanitizer
  features/login   account picker, password, tooltip
  features/dashboard  owned list and shared list
  features/editor  editing surface, toolbar, status bar, and a side panel with
                   three tabs: share, files, history
        |
        |  cookie session, XSRF token header
        v
Spring Boot
  web/       controllers, one exception handler, DTO records
  service/   DocumentAccessService, DocumentService, ShareService, VersionService,
             AttachmentService, DocumentImportService, HtmlSanitizer
  repo/      Spring Data repositories
  domain/    AppUser, Document, DocumentShare, DocumentVersion, Attachment,
             ShareRole, AccessLevel
        |
        v
  H2 file  or  Postgres
```

## The decisions that mattered

### One place decides access

`DocumentAccessService` is the only code that answers "what may this user do with
this document". It returns an `AccessLevel` of OWNER, EDITOR, VIEWER or NONE, and
exposes `requireReadable`, `requireWritable` and `requireOwner`.

Controllers never compare owner ids themselves. That is the whole point. Access
checks that are copy pasted into each endpoint are how the fifth endpoint ends up
with a subtly different rule, and the fifth endpoint is the one that leaks. There is
one rule, in one file, with tests pointed at it.

A user with no access to a document gets **404, not 403**. A 403 would confirm the
document exists, which turns a list of ids into a way to probe what other people are
working on.

### The editor stores HTML, not a document model

The editing surface is a `contenteditable` div. The toolbar drives
`document.execCommand`, and the resulting HTML is saved.

`execCommand` is formally deprecated. It is also implemented by every browser in use,
and the alternative inside this timebox is either pulling in a full editor framework
or writing a selection and range engine, which would have consumed the budget that
went into sharing and import.

The real cost of storing HTML is that there is no structured document model, which is
what real time collaborative editing needs. That is written down here rather than
discovered later, and it is the first thing that would change if collaboration moved
in scope.

Two details make this behave:

- The surface is written to **once**, on load, and never rebound. Binding `innerHTML`
  to a signal would push the caret back to the start on every keystroke.
- Because the browser is an untrusted source, everything is passed through jsoup with
  a tight allowlist before it reaches the database. Only the tags the toolbar can
  produce survive. A `<script>` tag pasted into the editor never gets stored.

### Paste is cleaned on the client as well as the server

The server allowlist is the check that protects the data, and it is not going
anywhere. But on its own it produced a bug worth describing, because it was invisible
until the app was actually used.

Paste a paragraph from Word. The browser inserts `<span style="color:red">`. It looks
right. The autosave fires, the server strips the attribute, and the text is black
again on the next reload. Nothing failed and nothing warned. The document simply
changed behind the user's back.

So pasted markup is now cleaned in the browser first, to the same tag set the server
keeps. Headings deeper than `h3` collapse to `h3` instead of vanishing, unsupported
wrappers like tables are unwrapped so their text survives, `javascript:` and `data:`
links lose their href but keep their text, and scripts go entirely.

The duplication is deliberate and is called out in a comment in both files. The client
copy exists for honesty about what will be saved. The server copy exists because the
client can be bypassed.

### Version history groups edits instead of recording every save

Autosave fires on every pause in typing. Snapshotting each one would bury the useful
history under hundreds of near identical rows, so consecutive saves by the same person
inside a two minute window update one entry rather than adding new ones. A different
author, or a gap longer than the window, starts a new version. This is the same idea
as the way Google Docs groups edits into sessions.

Two decisions follow from that:

- **History is append only.** Restoring version 3 does not delete versions 4 and 5. It
  writes version 3's content back onto the document, and that write becomes the newest
  version, tagged with what it was restored from. A restore can therefore be undone the
  same way as any other edit, which matters because restore is the one action in the
  app that can destroy someone else's work.
- **A restore is never coalesced.** It always gets its own entry, so the trail of who
  rolled back to what stays readable rather than merging into a neighbouring edit.

Reading history needs read access. Restoring needs write access, so a viewer can see
what changed but cannot change it.

### Saving is debounced, not immediate

Typing fires an input event per character. The save waits 900 ms after typing stops,
so a paragraph is one request instead of two hundred. The header shows Saving, then
All changes saved, so the user is never guessing.

Renames save on blur and on Enter, which is what people expect from a title field.

### Import and attachments are two different products

The brief allows one upload path. Two were built because they answer different needs.

**Import** turns a `.txt`, `.md` or `.docx` file into a new document you own.
Markdown goes through commonmark. Word files go through Apache POI, mapping heading
styles to `h1` and `h2`, numbered paragraphs to list items, and bold or italic runs
to `strong` and `em`. Plain text is escaped and split into paragraphs on blank lines.
Rejected types come back with a message naming the extension that was sent, not a
generic failure.

**Attachments** hang off an existing document, any type, 5 MB each. The bytes are
stored in the row. That is the wrong answer above a certain size and the right answer
here, because object storage would have meant a second service for reviewers to
configure. The limit is enforced in the service and stated in the UI.

Attachments are always served with `Content-Disposition: attachment`, never inline.
Rendering a user supplied file inline on the app origin is a stored cross site
scripting hole.

### Auth is a session cookie, not a JWT, and that is the right call here

This is worth stating plainly because JWT is often treated as the default modern
answer. For a browser SPA served from the same origin as its API, it is the weaker
option.

A JWT has to be stored somewhere the JavaScript can reach it, normally
`localStorage`. Anything that gets script onto the page can read it and walk away
with a credential that stays valid until it expires. An httpOnly cookie cannot be
read by script at all. The token approach also has no clean logout: a stateless JWT
stays valid until expiry unless you build a revocation list, at which point it is a
session, just with more moving parts.

JWTs earn their keep when the consumer is not a browser, when the API is called
across origins, or when several services need to verify a caller without sharing
session state. None of that is true here.

What is implemented:

| Concern | Approach |
| --- | --- |
| Password storage | BCrypt, through Spring Security's `PasswordEncoder` |
| Session | httpOnly, SameSite=Lax cookie, marked Secure under the prod profile |
| Session fixation | The session id is rotated on login |
| CSRF | Spring writes `XSRF-TOKEN`, Angular returns it in `X-XSRF-TOKEN` |
| User enumeration | A wrong password and an unknown email return the identical 401 |
| Brute force | Five failures locks an account for fifteen minutes |
| Transport | HSTS for a year, including subdomains |

The rate limiter keeps its counters in memory. That protects a single instance and
resets on restart, which is honest for this scope but would need Redis or a gateway
rule the moment there is more than one instance. It is written that way in the code
comment rather than left for someone to discover.

The one deliberate weak spot is `/api/auth/demo-users`, which lists the seeded
accounts to an anonymous caller so reviewers do not have to guess email addresses.
That is a user enumeration hole. It exists because this is a demo, it is labelled as
such in the code, and it disappears with the seeded accounts.

There is no signup. Four accounts are seeded on first start. Sharing needs several
users to be interesting, and a signup form would have taken time from the parts being
evaluated. The `/api/auth/demo-users` endpoint that fills the login page and the
people picker is a demo affordance and is labelled as such in the code.

### Lists do not load what they do not show

The dashboard renders a title, an owner and a timestamp. Loading `Document` entities
to build that pulled every document's full HTML body out of the database, then issued
one more query per row to resolve the lazy owner, and another per row to find the
share role. Listing twenty shared documents was forty one queries, several of them
carrying entire documents that were then discarded.

Both list endpoints now use a JPQL constructor projection that selects exactly the
columns the list draws. Measured on the same request with SQL logging on, it went
from five queries to three, and more importantly the count is now flat: it does not
grow with the number of documents. Version history got the same treatment, because
listing versions was loading every historical copy of the document just to render a
column of dates.

### Bytes belong in the row, not in a large object

`@Lob` on a `byte[]` looks harmless. On Postgres, Hibernate maps it to an `oid`,
which stores the bytes in the `pg_largeobject` system table and puts a pointer in the
row. Postgres does not cascade deletes into large objects, so deleting an attachment
frees the row and leaks the data forever.

This was verified rather than assumed. Against a real Postgres instance, uploading a
400 KB attachment and then deleting it left the `attachment` table empty and 400 KB
still sitting in `pg_largeobject`, unreachable by the application.

Declaring an explicit column length instead gives `bytea` on Postgres and `VARBINARY`
on H2. Both are stored in the row and both are removed with it. The same test now
shows zero orphaned objects after a delete. The `String` columns were checked at the
same time and were already correct, because `columnDefinition = "TEXT"` overrides the
`@Lob` mapping.

### Errors have one shape

Every failure leaves through a single `@RestControllerAdvice` as
`{ status, message, fieldErrors? }`. Validation lives on the request records as bean
validation annotations, so the message a user sees is written next to the rule it
belongs to.

On the client, `messageFrom` turns any failure into a sentence a person can act on,
including the case where the server cannot be reached at all. Unexpected exceptions
log the stack trace on the server and return a generic message, because stack traces
in a response body are a gift to whoever is probing the app.

The catch all handler was hiding a problem worth describing. `@ExceptionHandler(Exception.class)`
also swallows the typed exceptions Spring throws for ordinary client mistakes, so
four different bad requests were all coming back as 500:

| Request | Was | Now |
| --- | --- | --- |
| `/api/documents/abc` | 500 | 400, naming the bad value |
| `/api/nope` | 500 | 404 |
| A body sent as `text/plain` | 500 | 415 |
| `"role": "SUPERUSER"` | 500 | 400, listing the allowed values |
| An upload with no file part | 500 | 400, naming the missing field |

A 500 tells the caller nothing and tells the operator there is a bug when there is
not. The handler now maps each Spring exception explicitly and keeps the catch all
for what it is actually for, which is genuine faults.

### The production build is a different program

Two problems existed only in the packaged build, which is a reminder that a dev server
is not the thing being shipped.

**The content security policy broke the stylesheet.** Angular inlines critical CSS and
defers the rest with `<link media="print" onload="this.media='all'">`. The policy
forbids inline event handlers, so the handler never ran, the stylesheet stayed at
`media="print"`, and the deployed app rendered with component styles only and no
global stylesheet. Everything worked, it just looked broken, and nothing in dev mode
showed it. Turning off `inlineCritical` emits a plain link tag and the policy is
satisfied without weakening it.

**The app could not tell it was behind TLS.** Render and most hosts terminate HTTPS at
their edge and forward plain HTTP. Spring therefore treats every request as insecure,
which means it never emits the HSTS header and builds redirect urls with the wrong
scheme. `server.forward-headers-strategy: framework` makes it read the forwarded
headers instead.

Both were found by building the image and opening it in a browser rather than by
reasoning about the config.

## Tests

Tests were pointed at what is genuinely risky, not spread evenly for coverage.

**Backend, 42 tests**

- `DocumentAccessTest` drives the real HTTP layer. An owner reads and edits. A viewer
  reads and is refused an edit. An editor edits but cannot delete or reshare. An
  unrelated user gets 404 rather than 403. Sharing puts the document into the other
  user's shared list with the right role.
- `AuthFlowTest` covers sign in, session persistence across requests, a wrong
  password and an unknown email returning the identical message, and protected
  endpoints refusing an anonymous caller.
- `DocumentImportServiceTest` covers markdown structure, plain text paragraphs,
  escaping of markup in plain text, and rejection of unsupported types.
- `HtmlSanitizerTest` covers what survives and what does not.
- `ApiErrorHandlingTest` pins each malformed request to its correct status, so the
  catch all handler cannot start swallowing them again.
- `LoginAttemptServiceTest` covers the lock threshold, the reset on success, and
  that locking one account does not affect another.
- `VersionHistoryTest` covers the parts of history that are easy to get wrong: a
  burst of saves collapsing into one version, a second author starting a new one, a
  no op save recording nothing, a restore keeping the versions it rolled past, a
  viewer reading history but refused a restore, a stranger getting 404, a version id
  borrowed from another document being rejected, and deleting a document taking its
  history with it.

**Frontend, 69 tests**

- `html-clean.spec.ts` is the largest group, because the paste path is where a
  mistake is both silent and permanent. It asserts that supported formatting survives
  untouched, that styles and event handlers are stripped, that scripts, images and
  iframes are removed, that tables are unwrapped rather than dropped, that `h5`
  becomes `h3`, and that `javascript:` and `data:` hrefs are removed while `https:`
  and `mailto:` are kept.
- `documents.service.spec.ts` covers both directions: that an autosave sends only
  `contentHtml` and a rename only `title`, and that a 403, a 404, a 500 with no body,
  a 413 and an unreachable server each surface as the right message rather than being
  swallowed.
- `editor.component.spec.ts` covers the behaviour that only shows up in a rendered
  component: saved content reaching the surface, the document name reaching the
  browser tab, the debounce sending one request instead of two, a failed save
  reporting itself, a refused rename rolling the title back, and a viewer getting a
  read only surface with every toolbar button disabled and no save ever sent.
- `login.component.spec.ts` covers the failure paths, including an off site `next`
  parameter being ignored rather than followed.
- `share-panel.component.spec.ts` covers the picker excluding people who already have
  access and the user themselves, and the refusals surfacing.
- `confirm-dialog.component.spec.ts` covers the accessible roles, the destructive
  styling, and that Escape, the backdrop and Cancel all decline while a click inside
  the dialog does not.
- `relative-time.spec.ts` covers the boundaries and an unparseable date.

Beyond the automated tests, every flow was exercised by hand in a browser: sign in,
create, rename, format, autosave, reload to confirm persistence, paste styled markup
and confirm the stored copy matches the screen, nest a list item with Tab, import a
markdown file, attach and download a file, share with a role, restore an older version
and confirm the newer ones survived, and sign in as the recipient to confirm the
viewer sees a disabled toolbar.

## Deliberately not built

- **Real time collaboration.** Correct multi cursor editing needs operational
  transform or CRDTs plus a websocket layer and a structured document model. It does
  not fit the timebox, and a fake version that silently loses writes is worse than
  none.
- **Signup and password reset.** Seeded accounts serve the review better.
- **Comments and suggestion mode.** Each is its own feature.
- **Folders, search, tags.** No value at four documents.
- **Export to PDF.** Pleasant, but it demonstrates a library, not judgement.

## What I would do with another two to four hours

1. **Presence indicators.** Poll for who else has the document open and show avatars
   in the header. Honest about being presence only, not co editing, and roughly an
   hour.
2. **Optimistic concurrency.** Two editors on one document currently overwrite each
   other, last write wins. A version column on `Document` and a 409 on mismatch would
   at least surface the conflict instead of silently dropping work.
3. **Move attachment bytes out of the row.** A storage abstraction with a filesystem
   implementation locally and S3 in production, so the size cap can rise.
4. **Flyway migrations.** `ddl-auto: update` is right for a demo and wrong for
   anything that accumulates real data.
5. **A diff view between versions.** History currently shows each version whole.
   Showing what changed between two of them is the obvious next step and is where
   this feature stops being a rollback button and starts being a record.
6. **A retention rule for history.** Versions accumulate without limit. A real product
   would thin old ones out, keeping every version for a day, one an hour for a week,
   one a day after that.

# AI workflow note

## Tools used

- **Claude Code** in the terminal, as the main pair. Used for scaffolding, writing
  the bulk of the Java and TypeScript, and driving a browser to check the running app.
- **Angular CLI** for the project skeleton, so the generated config matches what an
  Angular reviewer expects rather than something hand rolled.
- No AI involvement in the product decisions. What to build, what to cut, and how to
  spend the timebox were settled first, on paper, before any code was generated.

## Where it materially helped

**Scaffolding that is boring but easy to get subtly wrong.** JPA entities, DTO
records, repository interfaces and the Spring Security wiring are all shapes I know
well and would have typed slowly. Generating them and reviewing the result was
several times faster than writing them by hand, and reviewing is where my attention
was actually useful.

**Keeping the two ends in step.** The TypeScript interfaces mirror the Java records.
Generating both from the same description removed a class of mismatch that normally
costs twenty minutes of confused debugging.

**Verification, not just typing.** The most valuable use was driving a real browser
against the running app: sign in, open a document, type into it, wait for the
autosave, sign out, sign back in as the viewer, confirm the toolbar is disabled. That
is exactly the tedious loop I would otherwise shortcut when the clock is running, and
it is where the real bugs were.

## What I changed or rejected

**The generated CSRF filter was registered in the wrong place.** It came back as a
`@Component`, which makes Spring Boot register it as a global servlet filter. It
would have run before the security chain and always seen a null token, so the
`XSRF-TOKEN` cookie would never have been written and every write request would have
failed with a 403. Moved it into the chain with `addFilterAfter(..., CsrfFilter.class)`.

**The editor was reading its content at the wrong moment.** The first version used
`ngAfterViewInit` plus a `queueMicrotask` to write saved content into the
`contenteditable` div. The div lives inside an `@if` block, so it does not exist yet
when the document loads. The app rendered every document as blank. I caught this by
opening the page rather than by reading the code, and replaced the `ViewChild` with a
setter that fires when the element actually appears.

**Rejected: binding the editor content.** The obvious Angular way is to bind
`innerHTML` to a signal. That resets the caret to the start on every keystroke. The
surface is written to once and read from on save. This is unidiomatic on purpose and
is commented in the code so it does not get "fixed" later.

**Rejected: a 403 for documents you cannot see.** The first pass returned 403 for a
document that exists but is not yours. That confirms the document exists. Changed to
404 and locked in with a test.

**The silent paste bug, found by testing rather than reading.** The first build passed
every test and looked correct. Pasting a styled paragraph from a web page showed the
colours on screen, and losing them on the next reload once the server had sanitized
the HTML. Nothing errored. I only caught it by pasting into the running app and then
comparing the DOM against what the API had actually stored. No amount of code review
would have surfaced that, because both halves were individually correct. The fix was
to clean on paste in the browser too, and the duplication that creates is commented in
both files.

**Rejected: trusting the first version history design.** The generated version was
straightforward, one snapshot per save. With a 900 ms autosave that is a new row every
time the user pauses, which makes the feature useless within a minute of typing. I
replaced it with a coalescing window and made restores append only rather than
destructive, because restore is the one action in the app that can throw away someone
else's work.

**The audit pass found things review would not have.** After the features were done I
went back over the whole thing looking for problems rather than for missing features,
and the useful findings all came from probing the running system, not from reading:

- Four ordinary client mistakes were returning 500, because
  `@ExceptionHandler(Exception.class)` also catches the typed exceptions Spring throws
  for a bad path variable, an unknown route, the wrong content type and an invalid
  enum value. Found by sending deliberately malformed requests with curl.
- A `@Lob byte[]` maps to a Postgres `oid`, which stores attachment bytes outside the
  row. I did not want to argue about this from memory, so I ran the app against a real
  Postgres in Docker, uploaded a 400 KB file, deleted it, and confirmed that the row
  was gone while 400 KB stayed behind in `pg_largeobject`. Fixed and re-verified to
  zero. The same test showed my suspicion about the text columns was wrong, they were
  already mapping correctly.
- The document lists were selecting every document's full HTML body to render a list
  of titles, then issuing a query per row for the owner and another per row for the
  share role. Confirmed by turning on SQL logging and counting.

- The content security policy I had just added silently broke the production
  stylesheet, because Angular defers CSS with an inline `onload` handler that the
  policy blocks. The dev server does not do this, so the only way to see it was to
  build the image, open it, and notice the page was unstyled. That one would have
  reached reviewers as a visibly broken deployment.

**Rejected: writing the tests to match the behaviour.** Two tests I added during the
audit failed, and both times the code was wrong rather than the test. A missing file
part was still a 500, which needed another handler. And the relative time helper used
`Math.round`, so thirty seconds displayed as "1 min ago". Both fixed in the code.

**Rejected: the deprecation reflex.** There was a suggestion to avoid `execCommand`
because it is deprecated. The realistic replacements are a full editor framework or a
hand written selection engine, and either would have taken the budget that went into
sharing and import. Kept it, with the tradeoff written into the architecture note
rather than left implicit.

**Trimmed the prose.** Generated comments and docs tend toward explaining what a line
already says. Comments in this repository explain why something is the way it is, and
the ones that only restated the code were deleted.

## How correctness was verified

**Automated.** 42 backend tests and 69 frontend tests, all passing. They are pointed
at the access rules, the auth flow, version history, file import and the paste
sanitizer, because that is where a mistake is both likely and expensive. Coverage was
not spread evenly for its own sake, and the largest single group of tests sits on the
paste path, because that is the bug that got past everything else.

**By hand in a browser.** Every flow was walked end to end, including reloading after
an edit to confirm the content was really persisted rather than held in memory, and
signing in as the viewer to confirm the read only state is real.

**By hand against the API.** Curl was used to check the error paths that are awkward
to trigger through the UI: an unsupported file extension, a title over the length
limit, sharing with an address that has no account, a malformed email, and a request
with no CSRF token. Each returns the right status and a readable message.

**Bugs found this way that tests alone would have missed.** The blank editor,
the silent paste rewrite described above, and a test that passed for the wrong
reason. An import assertion compared HTML exactly and failed because jsoup pretty
prints its output. The fix was to compare ignoring
whitespace. A generated test that had been written to match the buggy output would
have looked green and proved nothing.

## The honest summary

AI made this roughly two to three times faster, almost entirely on code volume and on
the verification loop. It did not make a single product decision, and its most
expensive suggestions, the filter placement, the view lifecycle, and the naive one
snapshot per save history, were all wrong in ways that only showed up by running the
thing or by thinking about what the feature is actually for. The review step is not
optional overhead. It is where the value gets captured.

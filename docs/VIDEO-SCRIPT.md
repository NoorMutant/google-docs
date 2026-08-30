# Walkthrough script

Target length five minutes. Have two browser windows open, one normal and one
private, so you can be signed in as two people at once without signing out on camera.

Before recording: start the backend and the frontend, open http://localhost:4200 in
both windows, sign in as Alice in the first and leave the second on the login page. Have a formatted web page or Word document
open in a third tab, ready to copy from.

---

## 0:00 to 0:25, what this is and what I chose

"This is a lightweight collaborative document editor built for the Ajaia assignment.
Angular 18 on the front, Spring Boot 3 on the back. It builds into one Docker image,
and I am running it locally here.

Google Docs is a decade of work, so the useful question was which slice carries the
most signal. Rather than build ten features shallowly, I went deep on two: the editing
experience, and version history. Sharing sits underneath both, with rules enforced on
the server rather than hidden buttons on the client."

## 0:25 to 0:50, sign in

Show the login page in the second window.

"There is no signup, four accounts are seeded. The login page lists them and the
password is in the tooltip, so a reviewer never has to guess. That is a deliberate
cut. Sharing needs several users to be interesting, and a signup form would have
taken time from the parts being evaluated.

Under the hood this is a real login, BCrypt hashed passwords, an httpOnly session
cookie, and CSRF tokens, not a mocked user."

## 0:50 to 1:40, the dashboard and the editor

Switch to the Alice window.

"Owned documents and shared documents are separate lists, and shared ones carry the
owner name and my role."

Open "Q3 product plan". Type a sentence.

"Bold, italic, underline, headings, both list types. Watch the header. It says Saving,
then All changes saved. Saves are debounced, so a paragraph is one request instead of
two hundred."

Press Tab inside a list item.

"Tab nests a list item, Shift Tab pulls it back out. The browser default is to move
focus out of the document entirely, which is useless in an editor. Word count is live
at the bottom."

Rename the document in the header, then reload the page.

"Reload, and it is all still there. That is real persistence, not local state."

## 1:40 to 2:15, paste, the bug worth talking about

Copy a formatted block from the third tab and paste it in.

"This one is worth showing because it is a bug I shipped and then found by using the
app rather than by reading the code.

The server strips anything outside the tag set it trusts, which is correct. But the
browser was happily showing me the pasted colours and fonts until the next reload,
when they silently vanished. Nothing errored. The document just changed behind me.

So paste is now cleaned in the browser too, to exactly the same tag set. Colours go,
structure stays, headings deeper than h3 collapse instead of disappearing, and a
javascript link loses its href but keeps its text. What you see is what gets stored."

Reload to show it is identical.

## 2:15 to 3:00, version history

Open the History tab.

"Every save is snapshotted. But autosave fires on every pause in typing, so one
snapshot per save would bury the history in hundreds of near identical rows. Edits made
close together by the same person are grouped into one version. A different author, or
a longer gap, starts a new one. That is the same idea as the way Google Docs groups
edits into sessions."

Click a version to preview it, then restore it.

"Preview in place, then restore. And the important part: history is append only.
Restoring does not delete the versions it rolled past. The restore itself becomes the
newest version, tagged with what it came from, so it can be undone the same way.

That matters because restore is the one action in this app that can throw away someone
else's work. Viewers can read the history but the restore button is not there for
them."

## 3:00 to 3:30, upload

Go back to the dashboard, click Import a file, pick a markdown file.

"Import turns a text, markdown or Word file into a new document you own. Markdown
goes through commonmark, Word goes through Apache POI and I map heading styles and
lists across, so the structure survives. Unsupported types are rejected with a message
that names the extension you sent."

Open the previous document, open the panel, attach a file.

"The second upload path is attachments on a document. Different need, so I built both.
Files are always served as a download, never rendered inline, because rendering user
supplied files on your own origin is a stored cross site scripting hole."

## 3:30 to 4:10, sharing

In the share panel, share with Dan as a viewer, then show Bob and Carol with their
roles.

"Viewer or editor. Only the owner can delete or change sharing."

Switch to the second window, sign in as Carol.

"Carol is a viewer. The document is under Shared with me, badged View only, and inside
it the toolbar is disabled with the reason stated.

This is not a hidden button. There is one service on the backend that decides what
you can do with a document, and every endpoint goes through it. If Carol posts an
edit directly to the API she gets a 403. If someone with no access at all asks for
the document they get a 404, not a 403, because a 403 would confirm the document
exists."

## 4:10 to 4:35, what I did not build

"No real time collaboration. Correct multi cursor editing needs operational transform
or CRDTs, a websocket layer, and a structured document model instead of the HTML I
store. It does not fit the timebox, and a fake version that silently loses writes is
worse than not having it.

Same reasoning for comments, suggestion mode and export. The known limit I would fix
first is that two editors still overwrite each other, last write wins. Version history
means the lost text is recoverable, but it does not prevent the clash."

## 4:35 to 5:00, AI and close

"AI wrote a lot of the code volume and, more usefully, drove a browser through every
flow to verify it. Three of its suggestions were wrong in ways only running the app,
or thinking about what the feature is for, would show. It registered a CSRF filter outside the security chain, which would have
broken every write request. It read the editor content at the wrong point in the
Angular lifecycle, so every document rendered blank. And its first version history
design wrote one snapshot per autosave, which makes the feature useless within a
minute of typing.

All three are in the AI workflow note. That is the honest summary. It made me two to three
times faster on volume, it made zero product decisions, and the review step is where
the value actually gets captured.

Tests are 26 on the backend, 56 on the frontend, all green, and they cover the
refusals as well as the working paths. Thanks for watching."

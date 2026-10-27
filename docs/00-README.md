# Wedding Planner — Planning Docs

An Android app (Kotlin + Jetpack Compose) for an engaged couple, CPP (groom) and CPW (bride), to plan their wedding. It syncs both ways with the couple's Google Sheet, a copy of the Indonesian "Wedding Checklist" template (tabs `DASHBOARD`, `WEDDING BUDGET`, `Undangan CPP/CPW`, …), which stays the source of truth.

## Using it for your own wedding

The docs are generic. Everything specific to one couple (names, account emails, event dates, the spreadsheet ID and tab IDs, the milestone dates, the WO) lives in **one file: [`wedding-profile.json`](../wedding-profile.json)** at the project root. Its schema is in [05-DATA-SCHEMA §7](05-DATA-SCHEMA.md#7-personal-configuration-wedding-profilejson).

1. Make a copy of the sheet template in your Google Drive and fill it in.
2. Edit `wedding-profile.json`: your names and display title, the account emails and roles, event dates, `spreadsheet.prodId` (and a `devTestCopyId`), plus `plan.kickoff` and the milestone dates.
3. Keep the file private. It's listed in `.gitignore`, and the build reads it to generate defaults (TRD §3.5).

## Documents

| # | Document | What's in it |
|---|---|---|
| 01 | [PRD](01-PRD.md) | Problem, goals, personas, metrics, feature IDs (HOME/BUD/CHK/VEN/GST/NTF/SET), user stories, MVP / v1.1 / Later, risks |
| 02 | [TRD](02-TRD.md) | Architecture and modules, Hilt, Google auth and scopes, backend decision (none), **two-way sync design**, notifications, security, testing, NFRs |
| 03 | [App Flow](03-APP-FLOW.md) | Navigation graph, onboarding, per-module flows, sync state machine, screen inventory (S-xx) |
| 04 | [Design Brief](04-DESIGN-BRIEF.md) | Tone, sage/cream M3 palette (light/dark), type, components, motion, accessibility, ASCII wireframes |
| 05 | [Data Schema](05-DATA-SCHEMA.md) | Enums, domain models, Room entities + ER diagram, sync tables, **full sheet ↔ entity mapping** |

## Key decisions at a glance

- **No backend.** Phones talk to the Sheets API directly; foreground polling every 60 s, background every 15 min.
- **Scope `spreadsheets`.** The OAuth app is published "In production" unverified, so tokens don't expire every 7 days.
- **Row identity:** a hidden `_id` in column A, with fingerprint reconciliation to repair sorts.
- **Conflicts:** a per-field three-way merge; real conflicts go to a Conflict centre.
- **Writes:** only to allowlisted input cells, re-checked by a pre-write guard just before writing. Formula cells are never written.
- **Defaults chosen for the 10 clarifying questions:** MVP in 4 weeks · may add hidden ID column · one partner owns the sheet, the other edits, both Gmail · publish unverified · only the couple edits, occasionally in Sheets · app may insert rows · guest pax in v1.1, WhatsApp later · vendors and event dates read-only first · both on Android, minSdk 28, Play internal testing · reminders to both at H-7/H-3/H-1, 08:00 WIB.

## Building and running (M0)

Needs JDK 17+ and Android SDK 36. From the project root:

```bash
./gradlew assembleDevDebug test
```

Before sign-in works, the sheet owner sets up Google Cloud once (manual; not automated):

1. Create a Google Cloud project and enable the **Google Sheets API**.
2. OAuth consent screen: External, add the `…/auth/spreadsheets` scope, add both partners as test users, then **Publish to production** (unverified; TRD §3.2).
3. Create OAuth clients: one **Web** client (put its ID in `google.webClientId`), and one **Android** client per package + signing SHA-1: `app.weddingplanner.dev` and `app.weddingplanner` (debug key: `keytool -list -v -keystore ~/.android/debug.keystore -storepass android`).
4. Make a copy of the sheet (File → Make a copy), put its ID in `spreadsheet.devTestCopyId`, and fill in the `accounts[].email` values.
5. Rebuild, sign in on the **dev** build, link the test copy, then open **Beranda → Penyiapan Sheet (dev)**: check the plan, run the migration, and confirm "Rumus di luar kolom A tidak berubah ✓". Run it on the real sheet only after that.

## Milestone plan

Targets are relative: **K** = kickoff, **W** = wedding day. The concrete dates live in `plan.milestones` in `wedding-profile.json`. The plan assumes about 12 weeks between kickoff and the wedding; stretch the gaps if you have more. Each milestone ends with a two-phone test on the **dev flavour against a test copy of the sheet**. The prod flavour is switched on after M2.

| Milestone | Target | Scope | Done when |
|---|---|---|---|
| **M0 — Sheet migration & skeleton** | K + 1 wk | The sheet owner makes the test copy of the sheet and shares it with the partner and the CI service account; project skeleton (modules, Hilt, theme); sign-in + link; debug "Penyiapan Sheet" screen runs the migration (column A `_id`, hidden + warning protection, `_APP_CONFIG`, `wp.schemaVersion=1`) | The migration is idempotent on the test copy and its formulas match before and after (FORMULA diff); then it runs once on prod |
| **M1 — Read-only sync + Home** | K + 2.5 wk | `SheetLayout` + parsers for every tab, header validation, pull into Room, Reconciler, sync pill, Home (HOME-1/3/4/5), Budget list/detail read-only, Vendor final read-only (VEN-1) | Home numbers equal the sheet's Progress Pembayaran box; a sort without column A on the test copy is repaired |
| **M2 — Budget edit + write-back → MVP** | K + 4 wk | PendingChange queue, Merger, PreWriteGuard, PushWorker; BUD-3/4/5 incl. formula-capable Price and row insert; Conflict centre; NTF-1 Jatuh Tempo reminders; Diagnostics | Integration suite green (conflict, insert, guard); 0 formula cells changed; **released to the internal track for both phones** |
| **M3 — Guests** | K + 6 wk | GST-1–5; GST-5 migration (guest column `I` Jumlah Orang, `schemaVersion=2`) | Counts equal rows 180–183; the bride's and groom's sides edited in parallel without conflicts |
| **M4 — Checklists** | K + 8 wk | CHK-1–5 (Pra Nikah, Pernikahan, Medical, Perawatan); Home checklist progress | Section % equals the sheet's recap blocks |
| **M5 — Vendors + milestone notifications → v1.1** | W − 2 wk | VEN-2/3, NTF-2/3, BUD-7, SET-5 WO view-only access (add WO to `_APP_CONFIG`, Drive Viewer share, internal testing) | Comparison matrix renders all blocks; event and Perawatan reminders fire; the WO's phone shows read-only data with no Budget |
| **Freeze** | W − 1 wk | Bug fixes only | — |

**This order differs from the suggested M3 Checklists → M4 Guests.** Guests come first because invitations usually go out 4–6 weeks before the wedding, and the checklists are typically well under way in the sheet and are easy to keep ticking there.

## Decisions on the open questions (26 Oct 2026)

Recorded as D1–D7 in [PRD §8](01-PRD.md#8-assumptions-decisions-and-risks).

| # | Decision |
|---|---|
| D1 | Home shows checklist progress for **CPP and CPW separately** |
| D2 | Overdue reminders: **daily at 08:00, at most 7 days** |
| D3 | Perawatan timeline anchored on the **Wedding date**; past steps don't fire |
| D4 | Tables are **rarely sorted** in Sheets; column A `_id` + reconciliation stays as a safety net |
| D5 | **The sheet owner creates the test copy** and shares it with the partner and the CI service account |
| D6 | **The WO gets view-only access about 2 weeks before the wedding** → new feature SET-5 in v1.1 (M5) |
| D7 | Guests get **only "Jumlah Orang"** (column `I`); no phone numbers |

## Open questions

1. **WO view (SET-5):** confirm with the WO that hiding all money figures, including Pembayaran status in Vendor, is fine, and that they use Android.

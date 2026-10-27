# 01 — Product Requirements Document

**Product:** Wedding Planner (working name, in-app title from `profile.displayTitle` in `wedding-profile.json`)
**Version:** 1.0 draft — 26 Oct 2026
**Owner:** the sheet owner (`people.sheetOwner` in `wedding-profile.json`)
**Related:** [02-TRD](02-TRD.md) · [03-APP-FLOW](03-APP-FLOW.md) · [04-DESIGN-BRIEF](04-DESIGN-BRIEF.md) · [05-DATA-SCHEMA](05-DATA-SCHEMA.md)

---

## 1. Problem

The couple plans the wedding in one Google Sheet, a copy of the Indonesian "Wedding Checklist" template. The sheet works well on a laptop. On a phone it doesn't:

- The tabs are wide and full of merged cells and formulas. Ticking a checkbox or updating a payment on mobile Sheets is fiddly, and it's easy to break a formula by accident.
- Nobody gets reminded. The budget tab works out "Jatuh tempo 14 hari", but someone has to open the sheet to see it.
- There's no quick answer to "how are we doing?". Countdown, progress and money owed live on three different tabs.

Couples usually adopt a tool like this with only a few months left; the plan assumes about **12 weeks** between kickoff and the wedding, with some events (e.g. Lamaran) possibly already behind them. The tool has to help now and can't be a long project.

## 2. Goals and non-goals

### Goals

| # | Goal |
|---|------|
| G1 | Make the everyday updates (record a payment, tick a checklist item, set a guest's RSVP) take under 10 seconds on a phone. |
| G2 | Show the state of the wedding on one Home screen: countdown, progress, money paid and owed, what's due next. |
| G3 | Remind both partners about Jatuh Tempo payments and H-x milestones without anyone opening the sheet. |
| G4 | Keep the Google Sheet as the single source of truth. Edits flow both ways and the sheet's formulas are never broken. |
| G5 | Work offline (at the venue, at a vendor's office) and sync later. |

### Non-goals (v1)

- iOS, web or tablet-optimised layouts.
- Replacing the sheet. Formatting, charts and the `DASHBOARD` calendars stay in Sheets.
- Users other than the couple and the WO's view-only access (SET-5). Family access and any other roles come *Later*.
- Sending invitations, payments or messages from the app.
- Editing the sheet's structure from the app (new categories, new checklist sections, new vendor-comparison blocks).

## 3. Personas

| | **CPP (Calon Pengantin Pria)** | **CPW (Calon Pengantin Wanita)** |
|---|---|---|
| Role | Groom. Typically the sheet owner and main budget keeper. | Bride. Owns attire, MUA, beauty prep and most of the CPW guest list. |
| Device | Android, power user | Android (**Assumption:** a recent phone, Android 9+) |
| Main jobs | Record DP and pelunasan, watch Sisa and Selisih Budget, chase vendors | Tick CPW checklist items, manage guests on the bride's side, track the Perawatan timeline |
| Frustration | Sheets mobile breaks merged layouts and formulas; he has to check due dates by hand | Doesn't want to "open the spreadsheet" to tick one thing |
| Success | "I can see what's owed this week at a glance." | "I can update my side in seconds and trust my partner sees it." |

Both partners have **full edit rights** to every module.

**Secondary persona: the WO (Wedding Organizer, `people.wo` in `wedding-profile.json`).** Gets **view-only** access from v1.1 (SET-5, about 2 weeks before the wedding) to follow the rundown-relevant state (checklists, vendors, guest counts) during the final weeks. Can't edit anything and doesn't see money.

## 4. Success metrics

| Metric | Target | How we measure |
|---|---|---|
| Weekly use | Both partners open the app ≥3×/week from MVP until the wedding | Local usage counter, shown in Settings → Tentang (no analytics SDK) |
| Edit share | ≥70% of budget and checklist edits made in the app, not in Sheets | Local counter vs. remote changes detected at sync |
| Sync integrity | 0 formula cells overwritten; 0 lost edits | Pre-write guard logs + conflict log; manual audit of the sheet's version history |
| Missed payments | 0 Jatuh Tempo dates missed | Every item with a due date gets a reminder |
| Sync latency | App edit visible in Sheets in under 10 s online; Sheet edit visible in app within 1 min while the app is open | Manual test with both phones |

## 5. Feature list by module

Feature IDs are used across all documents.

| Module | ID | Feature | Phase |
|---|---|---|---|
| **Onboarding** | ONB-1 | Sign in with Google (Credential Manager) | MVP |
| | ONB-2 | Link the sheet: prefilled ID, validate access and layout | MVP |
| | ONB-3 | First sync with progress | MVP |
| **Home** | HOME-1 | Countdown to Wedding (H-x, days) and the next upcoming event | MVP |
| | HOME-2 | Overall progress: checklists (CPP/CPW) and payments | MVP (payments) / v1.1 (checklists) |
| | HOME-3 | Payment summary: Total Biaya, Sudah Dibayar, Sisa, Progress, Item Lunas | MVP |
| | HOME-4 | Upcoming due payments (Jatuh Tempo within 14 days, plus overdue) | MVP |
| | HOME-5 | Budget health: Budgeting Nikah vs. Total Estimasi, Selisih Budget | MVP |
| **Budget & Payments** | BUD-1 | List Akad & Resepsi and Mahar & Seserahan items; filter by category and status; search | MVP |
| | BUD-2 | Item detail: every field plus derived Total, Sisa, Progress, Status | MVP |
| | BUD-3 | Record a payment (update Sudah Dibayar) with quick actions "Bayar DP" and "Lunasi" | MVP |
| | BUD-4 | Edit an item's input fields (category, detail, vendor, qty, unit, price, due date, notes) | MVP |
| | BUD-5 | Add an item into a reserved empty row; remove an item (clears its row) | MVP |
| | BUD-6 | Category recap (allocation per category, %) | MVP |
| | BUD-7 | Edit Budgeting Nikah and the budget-split % | v1.1 |
| **Checklists** | CHK-1 | Checklist Pra Nikah (Lamaran, Pengajian/Siraman): sections → sub-sections → items with CPP and CPW checkboxes | v1.1 |
| | CHK-2 | Checklist Pernikahan: Administrasi & Legalitas, and Persiapan Akad & Resepsi by category | v1.1 |
| | CHK-3 | Medical check-up and Perawatan timeline (single checkbox) | v1.1 |
| | CHK-4 | Edit Detail/Keterangan and PIC | v1.1 |
| | CHK-5 | Progress per section and per partner; filter "Belum selesai (saya)" | v1.1 |
| | CHK-6 | Add or remove checklist items | Later |
| **Vendors** | VEN-1 | Finalisasi Vendor list: category, vendor, Pembayaran (derived), Pertemuan, notes (read-only) | MVP |
| | VEN-2 | Edit Pertemuan and Keterangan Lain | v1.1 |
| | VEN-3 | Vendor comparison per category, as a matrix (read-only) | v1.1 |
| | VEN-4 | Edit comparison values, add candidate vendors | Later |
| **Guest List** | GST-1 | Guest lists for CPP and CPW; search; filter by Kategori, Sub-Kategori, Hadir, invitation type | v1.1 |
| | GST-2 | Edit a guest: Kategori, Sub-Kategori, Nama, Undangan Digital, Undangan Fisik, Hadir | v1.1 |
| | GST-3 | Add and remove guests | v1.1 |
| | GST-4 | Counts per side and combined: Ya / Mungkin / Tidak per column | v1.1 |
| | GST-5 | New field: Jumlah Orang (pax), which needs a sheet migration (one new column) | v1.1 |
| | GST-6 | Share an invitation via WhatsApp intent | Later |
| **Notifications** | NTF-1 | Jatuh Tempo reminders at H-7, H-3 and H-1 at 08:00 WIB, plus an "overdue" nudge | MVP |
| | NTF-2 | Event milestone reminders (Pengajian, Wedding) and Perawatan timeline H-x | v1.1 |
| | NTF-3 | Choose which reminders you get | v1.1 |
| **Settings / Sync** | SET-1 | Sync status, "Sinkronkan sekarang", last synced time | MVP |
| | SET-2 | Conflict centre: resolve conflicting edits | MVP |
| | SET-3 | Account, linked sheet, sign out, clear local data | MVP |
| | SET-4 | Diagnostics: layout validation result, pending queue, export logs (no PII) | MVP |
| | SET-5 | WO access, view-only (`VIEWER` role): read-only UI, Budget hidden | v1.1 |

## 6. User stories and acceptance criteria

Format: *As a … I want … so that …* followed by Given/When/Then criteria. "Partner" means either CPP or CPW.

### Onboarding

**ONB-1 Sign in.** As a partner I want to sign in with my Google account so that the app uses the sheet access I already have.
- Given a fresh install, when I tap "Masuk dengan Google", then the Credential Manager sheet shows my Google accounts and I can pick one.
- Given I picked an account, when the app asks for spreadsheet access, then I see the Google consent screen once. If I decline, I see "Akses Google Sheets diperlukan" and a retry button.
- Signing out removes local data and revokes the app's token.

**ONB-2 Link the sheet.** As a partner I want the app to connect to our sheet so that I don't have to configure anything.
- The sheet ID comes prefilled from the build variant. I can also paste a sheet URL instead.
- When I tap "Hubungkan", the app checks that (a) my account can open the sheet, (b) every required tab exists, and (c) the header rows match the expected layout.
- If my account can't open the sheet, I see "Akun ini belum punya akses ke sheet. Minta pemilik sheet untuk membagikannya."
- If the layout doesn't match, I see which tab or header differs, and the app won't sync that module (the others still sync).

**ONB-3 First sync.** As a partner I want to see the data quickly so that I trust the app.
- The first sync finishes in under 10 s on 4G and shows a progress indicator per module.
- When it finishes, I land on Home with real numbers that match the sheet.

### Home

**HOME-1 Countdown.** As a partner I want to see how many days are left so that we pace ourselves.
- Home shows "H-83" and "83 hari lagi menuju hari bahagia" computed in the couple's time zone (`profile.timeZone`, default `Asia/Jakarta`), using the Wedding date from `DASHBOARD!O10`.
- It also shows the next event that hasn't happened yet (e.g. Pengajian/Siraman) and how many days remain.
- On and after the wedding day the card shows "Hari ini!" / "Selamat menempuh hidup baru".

**HOME-3 Payment summary.** As CPP I want the totals so that I know where money stands.
- Shows Total Biaya, Sudah Dibayar, Sisa Pembayaran, Progress (%) as a bar, and "Item Lunas: 10 dari 38 item".
- The values match the sheet's Progress Pembayaran box after a sync. Pending local edits are reflected immediately and marked "belum tersinkron".

**HOME-4 Upcoming due.** As a partner I want to see due payments so that nothing slips.
- Lists items whose Status ≠ Lunas and whose Jatuh Tempo is ≤14 days away or already passed. Overdue items come first, in red, and each row shows Sisa.
- Tapping a row opens that budget item. If the list is empty: "Tidak ada pembayaran jatuh tempo dalam 14 hari".

**HOME-5 Budget health.** Shows Budgeting Nikah, Total Estimasi Budget, Selisih Budget and "Budget terpakai %". If Selisih is negative, the card turns into a warning.

### Budget & Payments

**BUD-1 List.** As a partner I want to browse budget items so that I find things fast.
- There are two tabs, "Akad & Resepsi" and "Mahar & Seserahan". Each row shows Detail Item, Vendor, Total, Sisa and a Status chip (Lunas / DP / Belum bayar).
- I can filter by category (the sheet's dropdown values) and status, sort by Jatuh Tempo / Total / No, and search by detail or vendor.
- Items with a missing price show "Harga belum diisi", matching the sheet's yellow highlight.

**BUD-3 Record payment.** As CPP I want to record a payment in two taps so that the sheet stays current.
- On an item I can tap "Catat Pembayaran" and enter an amount to **add** to Sudah Dibayar, or set the total paid directly.
- "Lunasi" sets Sudah Dibayar = Total after a confirmation.
- Status, Sisa and Progress update immediately (computed locally). After the sync they show the sheet's values.
- If the payment would make Sudah Dibayar > Total, I get a warning but can still save.
- The app writes only `J` (Sudah Dibayar) and optionally `N` (Jatuh Tempo, the next due date) and `O` (Notes).

**BUD-4 Edit item.** As a partner I want to correct an item's details.
- I can edit Category (strict list), Detail Item, Vendor (Akad) or Groom/Bride (Mahar), Qty, Unit, Price, Jatuh Tempo and Notes.
- If Price holds a formula (e.g. `=1300000+800000+600000`), the field shows the expression and its result. I can keep it, edit the expression, or replace it with a plain number.
- Total, Sisa, Progress and Status are never editable.

**BUD-5 Add / remove.** As a partner I want to add new costs.
- "Tambah Item" writes into the first empty reserved row of the right table (Akad rows 26–75, Mahar rows 81–106).
- When no reserved rows are left, the app inserts a row inside the table and copies the formulas down, then tells me "Baris baru ditambahkan di sheet".
- "Hapus" asks for confirmation, then clears the row's input cells and its ID. Formulas and the row stay in place.

### Checklists

**CHK-1/2 Dual checklist.** As a partner I want to tick my own column so that we each track our part.
- Items are grouped exactly as in the sheet (section → sub-section → item) and the groups can be collapsed.
- Each item shows two checkboxes labelled CPP and CPW. The signed-in partner's own column is highlighted (**Assumption:** each account is mapped to CPP or CPW in Settings once).
- Tapping a checkbox saves instantly (optimistic) and syncs.
- The section header shows "CPP 41% · CPW 40%".

**CHK-3 Medical & Perawatan.** Single checkbox per item. Perawatan items are grouped by Waktu (H-6 Bulan, H-3 Bulan, H-1 Bulan, H-5/H-3 Hari, H-2/H-1 Sebelum Nikah). Rows that fall under a merged Waktu cell inherit it.

**CHK-4 Detail/PIC.** I can edit Detail/Keterangan and PIC from the item's bottom sheet.

**CHK-5 Filter.** "Belum selesai (saya)" shows the items still open in my column.

### Vendors

**VEN-1 Finalisasi.** As a partner I want to see who we've booked.
- Lists the 9 categories from `Perbandingan Vendor!B6:B14` with Nama Vendor, Pembayaran chip and Pertemuan chip.
- Tapping a vendor that also appears in the budget (exact match on name) opens the matching budget items.

**VEN-3 Comparison.** I see each comparison block as a matrix: criteria are rows, candidate vendors are columns, booleans show ✓/✗, and Catatan PLUS/MINUS appear as text. Empty blocks show "Belum ada vendor yang dibandingkan".

### Guest List

**GST-1/2 Guests.** As CPW I want to manage the bride's side.
- The CPP and CPW tabs are separate lists, with a "Semua" tab that combines both.
- Each row shows Nama, Sub-Kategori, and chips for Digital, Fisik and Hadir. I can change Hadir in one tap (Ya → Mungkin → Tidak → kosong).
- Editing keeps No. (`B`), which comes from the sheet's ARRAYFORMULA, untouched.

**GST-3 Add.** A new guest goes into the first empty row in `3–178` of that side's tab. If that range is full, the app inserts a row above the count block (row 179) and the count formulas grow with it.

**GST-4 Counts.** Shows Undangan Digital Ya, Undangan Fisik Ya, Hadir Ya / Mungkin, and a total, per side and combined. The numbers match rows 180–183.

**GST-5 Jumlah Orang.** As a partner I want to record how many people a guest entry covers (e.g. "Om & Tante" = 2) so that we can estimate catering.
- The edit screen has a "Jumlah Orang" stepper (1–10, default 1), stored in the new guest column `I`.
- Counts show "Perkiraan hadir: n orang", the sum of pax over guests with Hadir = Ya, plus a range that also includes Mungkin.
- Phone numbers are **not** collected (decision 26 Oct 2026), so no extra personal data is stored.

### Notifications

**NTF-1 Jatuh Tempo.** As a partner I want reminders before payments are due.
- For every budget item with Jatuh Tempo and Status ≠ Lunas, both partners get notifications at H-7, H-3 and H-1 at 08:00 WIB, plus one on the day: "Jatuh tempo hari ini: Catering — Sisa Rp20.000.000".
- If an item passes its due date unpaid, a daily overdue nudge fires at 08:00 for at most 7 days, stopping as soon as it's Lunas (D2).
- Reminders are rescheduled after every sync and cancelled when an item becomes Lunas or its date is removed.
- Tapping a notification opens the item.

**NTF-2 Milestones.** Reminders at H-30, H-7 and H-1 for Pengajian/Siraman and Wedding, plus the Perawatan timeline (e.g. "H-1 Bulan: Treatment Gigi") anchored on the **Wedding date** (D3). Steps already in the past get no reminder. Viewers (SET-5) get only the event milestones.

### Settings / Sync

**SET-1 Sync status.** A status pill is always visible in the top app bar: Tersinkron / Menyinkronkan… / Offline (n perubahan menunggu) / Konflik / Gagal. Tapping it opens Sync details.

**SET-2 Conflicts.** When the same field was changed in the app and in the sheet since the last sync, I see both values ("Di aplikasi" vs "Di Google Sheet") and choose one. Until I choose, the app shows the sheet's value.

**SET-5 WO view-only access.** As the WO I want to see the couple's progress so that I can coordinate the final weeks without asking.
- The sheet owner adds the WO's email to `_APP_CONFIG` with role `VIEWER`, shares the sheet with them as **Viewer** in Drive, and adds them to the Play internal testing list. No app release is needed.
- On sign-in, a `VIEWER` skips "Siapa kamu?" (S-04) and gets a read-only app: no FABs, checkboxes, chips or edit forms, and a banner "Mode lihat saja".
- The **Budget tab and every money figure are hidden** (Home shows the countdown, events and checklist progress only). **Assumption:** the WO doesn't need payment data in the app; Pembayaran chips in Vendor are hidden too.
- The WO sees Checklists, Vendor (final + comparison) and Guests (list and counts).
- The WO gets event milestone reminders (NTF-2) only, not Jatuh Tempo.
- Enforcement is Drive's: a Viewer's write would fail with 403. The app just doesn't offer writes.

## 7. Scope split

| Phase | Content | Why |
|---|---|---|
| **MVP** (target: kickoff + 4 weeks) | ONB-1–3, HOME-1/3/4/5, HOME-2 (payments), BUD-1–6, VEN-1, NTF-1, SET-1–4 | Money is where mistakes hurt most, and most remaining payments fall due in the last two months. Budget is also the hardest sync case (formulas, inserts), so it proves the sync engine early. VEN-1 is a cheap read and links vendors to the budget. |
| **v1.1** (target: wedding − 2 weeks) | GST-1–5, CHK-1–5, VEN-2/3, HOME-2 (checklists), BUD-7, NTF-2/3, SET-5 | Guests come first (invitations go out 4–6 weeks before the wedding), then checklists. Both are simple checkbox/text writes once the sync engine is proven. SET-5 lands in M5 so the WO is on board in the final weeks, when coordination peaks. |
| **Later** | CHK-6, VEN-4, GST-6, family access and other roles, home-screen widget, post-wedding mode | These need structural sheet edits or new auth flows, and they matter less with 2 weeks to go. Freeze the app one week before the wedding. |

## 8. Assumptions, decisions and risks

### Assumptions

- **Assumption:** The sheet is owned by one partner's personal Gmail (`accounts[role=OWNER]`) and shared with the other as **Editor**. Both use personal Gmail accounts, not Workspace.
- **Assumption:** Only the couple edits the sheet, and edits made directly in Sheets are occasional. From v1.1 the WO also *reads* it (Drive Viewer).
- **Assumption:** The WO uses an Android phone. If not, they keep using the sheet directly with Viewer access.
- **Assumption:** Both phones run Android 9 (API 28) or newer. The app is distributed through Play Console internal testing.
- **Assumption:** We may add a hidden `_id` column (column A) to the Budget, Checklist, Medical and Guest tabs, plus a spreadsheet-level `DeveloperMetadata` schema version (see TRD §5.3).
- **Assumption:** The app may insert rows when the reserved rows run out and copy formulas down.
- **Assumption:** Each Google account is mapped once to a partner side (CPP or CPW). The mapping is stored in a hidden `_APP_CONFIG` tab so both phones agree.
- **Assumption:** Tables are rarely sorted in Sheets (confirmed 26 Oct 2026); fingerprint reconciliation is a safety net, not a routine path.

### Decisions (resolved 26 Oct 2026)

| # | Question | Decision |
|---|---|---|
| D1 | How HOME-2 shows checklist progress | **CPP and CPW separately**, as the sheet's recap does. No combined "both ticked" figure. |
| D2 | Overdue reminder frequency | **Daily at 08:00 WIB, for at most 7 days** after Jatuh Tempo, or until Lunas. |
| D3 | Perawatan timeline anchor | **Wedding date.** Steps already in the past at kickoff get no reminder. |
| D4 | Sorting in Sheets | **Rarely or never.** Keep the column A `_id` + reconciliation as a safety net. |
| D5 | Test copy of the sheet | **The sheet owner creates it** in their Drive and shares it with the partner and the CI service account. |
| D6 | WO access before the wedding | **Yes, view-only about 2 weeks before the wedding** → SET-5 in v1.1 (M5). Family access stays *Later*. |
| D7 | New guest columns (GST-5) | **Only "Jumlah Orang"** (column `I`). No phone numbers. |

### Open questions

None blocking. The SET-5 assumption (the WO sees no money) is the one to confirm with the WO.

### Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Someone changes the sheet layout (inserts a column, renames a header) | Wrong cells read or written | Header validation on every sync. A mismatch blocks writes to that module (TRD §5.2). |
| A table is sorted in Sheets without the hidden `_id` column | Rows lose their identity | Fingerprint reconciliation repairs IDs (TRD §5.3). Tell both partners to sort whole rows only. |
| Limited time until the wedding (~12 weeks) | MVP arrives late and loses value | Hard scope cuts. MVP = budget first. Freeze one week before the wedding. |
| OAuth app published unverified | "Unverified app" warning; 100-user cap | Acceptable for 3 users (couple + WO). Refresh tokens don't expire, unlike Testing mode. |
| The WO sees personal data (guest names) | Third-party exposure | Guests are visible to the WO by design, because they need them for coordination; access is a revocable Drive share, and removing the `_APP_CONFIG` row hides the app's data on the next sync. |
| Editing in both places at once | Lost edits | Per-field three-way merge with a conflict centre (TRD §5.5). |
| Guest names are personal data | Privacy exposure if a phone is lost | No cloud copy outside Google; local DB excluded from backups; no PII in logs (TRD §7). |

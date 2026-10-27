# 04 — Design Brief

**Version:** 1.0 draft — 26 Oct 2026
**Related:** [01-PRD](01-PRD.md) · [03-APP-FLOW](03-APP-FLOW.md) (screen IDs `S-xx`)

---

## 1. Tone and feel

**Warm, calm, celebratory, but practical.** The app should feel like a well-kept wedding binder, not a finance app. In practice:

- **Calm by default.** Cream surfaces, sage accents, lots of whitespace, one focal number per card.
- **Celebratory in moments, not everywhere.** The countdown hero, "Lunas" confirmations and completed sections get a small flourish (a serif headline, a gentle confetti burst once per section). Lists and forms stay plain.
- **Practical first.** Money and due dates are always legible at a glance. Red is used only for overdue items and errors.
- **Speaks like the sheet.** Bahasa Indonesia, using the sheet's own words: Sudah Dibayar, Sisa, Lunas, DP, Belum bayar, Jatuh Tempo, CPP, CPW, Hadir. Copy is warm but short: "Pembayaran dicatat", "Semua tugasmu selesai 🎉".

Built on **Material 3** with a custom colour scheme, shapes and type. Dynamic colour (Material You) is **off**, because the sage/cream identity matters more here than matching the wallpaper.

## 2. Colour

Built around the sheet's sage `#6B803F` and cream `#FFFDE7`. The raw sage gives only 4.4:1 against white, so text and buttons use a slightly deeper sage (`#56692E`, 6.1:1). The original sage is kept for large non-text elements such as progress fills and illustrations (≥3:1).

### 2.1 Light theme

| Role | Hex | Use |
|---|---|---|
| `primary` | `#56692E` | Buttons, active nav, links, checkbox fill |
| `onPrimary` | `#FFFFFF` | |
| `primaryContainer` | `#D9E8B5` | Selected chips, countdown hero background |
| `onPrimaryContainer` | `#1A2600` | |
| `brandSage` (custom) | `#6B803F` | Progress fills, rings, illustration |
| `secondary` | `#6E5D3E` | Secondary buttons, category icons |
| `secondaryContainer` | `#F3E2C0` | Filter chips (selected) |
| `tertiary` (gold) | `#8A6D1F` | Celebratory accents, H-x numeral, event icons |
| `tertiaryContainer` | `#FCE7A8` | |
| `background` / `surface` | `#FFFDE7` | App background (sheet cream) |
| `surfaceContainerLow` | `#FAF7DF` | Cards |
| `surfaceContainer` | `#F4F1D8` | Bottom sheets, nav bar |
| `surfaceContainerHigh` | `#EEEBD2` | Text field fill |
| `onSurface` | `#1C1C14` | Body text (17:1 on cream) |
| `onSurfaceVariant` | `#47483B` | Secondary text (9:1) |
| `outline` | `#77786A` | Field borders (4.4:1, borders only) |
| `outlineVariant` | `#C8C7B5` | Dividers |
| `error` | `#BA1A1A` | Overdue, errors |
| `errorContainer` | `#FFDAD6` | |

### 2.2 Dark theme

| Role | Hex |
|---|---|
| `primary` | `#B5C98A` |
| `onPrimary` | `#263500` |
| `primaryContainer` | `#3E5018` |
| `onPrimaryContainer` | `#D9E8B5` |
| `brandSage` | `#9DB36A` |
| `secondary` | `#D9C6A0` |
| `tertiary` | `#E5C46A` |
| `background` / `surface` | `#14140F` |
| `surfaceContainerLow` | `#1C1C17` |
| `surfaceContainer` | `#21211B` |
| `surfaceContainerHigh` | `#2B2B25` |
| `onSurface` | `#E6E3D1` |
| `onSurfaceVariant` | `#C8C7B5` |
| `outlineVariant` | `#47483B` |
| `error` | `#FFB4AB` |

### 2.3 Semantic colours (both themes)

These mirror the sheet's own dropdown chip colours, so the app and the sheet look alike.

| Semantic | Light container / content | Dark container / content |
|---|---|---|
| **Lunas** | `#D4EDBC` / `#11734B` | `#1E4D33` / `#B6E3A0` |
| **DP** | `#FFE5A0` / `#473821` | `#5A4712` / `#FFE08A` |
| **Belum bayar** | `#FFCFC9` / `#B10202` | `#6B1A14` / `#FFB4AB` |
| **Pertemuan: Selesai / Sudah Terjadwal / Belum Terjadwal** | same as Lunas / DP / Belum bayar | same |
| **CPP marker** | `#3E6497` (slate blue, 6.1:1) | `#A9C7FF` |
| **CPW marker** | `#B4536E` (rose, 4.8:1) | `#FFB1C4` |
| **Hadir: Ya / Mungkin / Tidak** | Lunas / DP / neutral (`#E6E3D1` / `#47483B`) | same mapping |

CPP/CPW colours are **identity markers** (checkbox ring, small avatar dot). They're never the only signal, because every marker also carries the text "CPP"/"CPW".

## 3. Typography

| Token | Font | Size / line height | Use |
|---|---|---|---|
| `displayLarge` | **Fraunces** SemiBold, opsz 72 | 56 / 64 | Countdown numeral "83" |
| `headlineMedium` | Fraunces Medium | 28 / 36 | Screen titles on Home and Welcome; the couple title (`profile.displayTitle`) |
| `titleLarge` | **Plus Jakarta Sans** SemiBold | 22 / 28 | Top app bar, detail titles |
| `titleMedium` | Plus Jakarta Sans SemiBold | 16 / 24 | Card titles, list primary text |
| `bodyLarge` | Plus Jakarta Sans Regular | 16 / 24 | Body, fields |
| `bodyMedium` | Plus Jakarta Sans Regular | 14 / 20 | List secondary text |
| `labelLarge` | Plus Jakarta Sans SemiBold | 14 / 20 | Buttons, chips |
| `labelSmall` | Plus Jakarta Sans Medium | 11 / 16 | Pills, "belum tersinkron" |
| `money` (custom) | Plus Jakarta Sans SemiBold, `tnum` | 16–28 | Every Rupiah figure, right-aligned in lists |

Plus Jakarta Sans was designed in Indonesia and handles Bahasa Indonesia text well. Fraunces gives a soft, invitation-like serif for the few celebratory spots. Both ship in the APK (no Google Fonts call at runtime, which matters offline). The serif is never used below 24sp.

## 4. Shape, spacing, elevation

- **Shapes:** small 8dp (chips, fields), medium 16dp (cards), large 28dp (bottom sheets, countdown hero). Chips are fully rounded.
- **Spacing:** 4dp grid; screen gutters 16dp; card padding 16dp; 12dp between cards.
- **Elevation:** tonal (surface containers) instead of shadows. Only the FAB and bottom sheets get shadow level 1–2.

## 5. Components

| Component | Spec |
|---|---|
| **ProgressRing** | 120dp on Home (stroke 12dp), 40dp in lists. Track `outlineVariant`, fill `brandSage`. Centre shows "21,7%" in `money`. Animates from the previous value (600 ms, emphasised-decelerate). At 100% the fill becomes `tertiary` gold, with a one-time sparkle. |
| **ProgressBar (linear)** | 8dp, rounded. Used in budget rows (Progress %) and section headers. Dual variant for checklists: two thin bars stacked, CPP (blue) over CPW (rose), each labelled with its %. |
| **StatusChip** | Assist-chip shape, 28dp high, colours from §2.3, a leading icon (✓ Lunas, ◐ DP, ○ Belum bayar) and text. Read-only (status is derived), so no ripple. |
| **PertemuanChip / HadirChip** | Same visuals. HadirChip is tappable: it cycles values, with a haptic tick and the chip's accessibility label reading "Hadir: Mungkin, ketuk untuk mengubah". |
| **RupiahField** | Outlined text field, `Rp` prefix, numeric keyboard, live grouping `1.000.000` while typing (thousand separator "."). Stores `Long`. A "Pakai rumus" toggle switches to an expression keyboard (`+ − × ÷`) with a live result below ("= Rp1.750.000"). Error state for negatives. |
| **Money text** | `Rp1.000.000` (no space, "." grouping, no decimals). Large values in cards may be compacted: `Rp20 jt`. Lists always show the full figure. |
| **DualCheckbox** | Two 48dp touch targets side by side, labelled "CPP" / "CPW" underneath (labelSmall). Ring colour is the side marker colour; fill is `primary` when checked. The signed-in partner's column has a subtle `primaryContainer` pill behind it. Animated tick (150 ms). |
| **SingleCheckbox row** | Medical/Perawatan. Standard M3 checkbox, 48dp. |
| **SyncPill** | In the top app bar, end side. Height 28dp, icon + label (§4 of App Flow). Pending changes show a count badge. |
| **DueRow** | Leading date block ("12 / DES"), title, Sisa (money), trailing "3 hari" chip. Red date block when overdue or ≤3 days; amber when ≤14 days. |
| **Pending marker** | 14dp cloud-off icon in `onSurfaceVariant` next to the edited value until it's confirmed. Conflict uses an amber warning icon. |
| **Empty states** | Small line illustration in `brandSage` (rings, a jasmine sprig, an envelope), one line of copy, one action. |

## 6. Iconography

Material Symbols **Rounded**, weight 400, grade 0, fill 0 (fill 1 for the active nav). Domain icon map:

| Concept | Symbol |
|---|---|
| Beranda | `favorite` |
| Anggaran | `account_balance_wallet` |
| Checklist | `checklist` |
| Tamu | `groups` |
| Lainnya | `more_horiz` |
| Vendor | `storefront` |
| Venue / Catering / Decor / Attire & Make Up / Photo & Video / Invitation & Souvenirs / Entertain / Talent / Transport / Administrasi / Organizer / Other | `location_on` / `restaurant` / `local_florist` / `checkroom` / `photo_camera` / `mail` / `mic` / `directions_car` / `description` / `event_note` / `category` |
| Mahar / Seserahan | `diamond` / `redeem` |
| Jatuh tempo | `event_upcoming` |
| Sync states | `cloud_done`, `sync`, `cloud_off`, `warning`, `error`, `lock` |

## 7. Motion

- Standard M3 easing. Durations: 150 ms (checkbox, chip), 300 ms (bottom sheet, card expand), 600 ms (progress ring).
- Navigation: shared-axis X between tabs, container transform from list row to detail (S-20 → S-21).
- **Celebrations** (respect "Remove animations"): confetti for 1.2 s when an item becomes Lunas through BUD-3, or when a checklist section reaches 100% for the signed-in partner. Once per event, never on sync-driven changes.
- With reduced motion, rings and bars jump to their values and confetti is replaced by a static ✓ badge.

## 8. Accessibility

- **Contrast:** body text ≥4.5:1 and large text/icons ≥3:1 in both themes (values in §2 were picked against those thresholds). Chips use text + icon, not colour alone.
- **Touch targets:** ≥48×48dp, including each half of DualCheckbox and each HadirChip.
- **TalkBack:** every checkbox announces item + side + state ("Fotocopy KTP, CPW, belum selesai"). Rupiah figures are read as words ("19 juta 500 ribu rupiah") through a custom `semantics { contentDescription }`. Rows merge their descendants so a list item is one focus stop with actions.
- **Font scale:** layouts tested at 100%, 130% and 200%. Money columns wrap under the title instead of truncating. The countdown numeral is capped at 1.3× scale.
- **Dark theme** follows the system, with an override in Settings later if needed.

### 8.1 Read-only mode (WO, SET-5)

- A persistent `secondaryContainer` banner under the top app bar: eye icon + "Mode lihat saja".
- Checkboxes render as static icons (✓ / ○) with no ripple and no 48dp touch target; chips become plain labels. Screen readers announce the state only ("Mahar, CPP, sudah selesai"), never "ketuk untuk mengubah".
- No FABs, no edit or overflow actions. The bottom bar has 4 items (Anggaran removed). Home hides the payment and budget cards, so the countdown hero is followed directly by checklist progress.
- Vendor rows hide the Pembayaran chip and comparison Harga rows still show, since they're quotes rather than payments. **Assumption:** confirm with the WO.

## 9. Key screens

Wireframes are 360dp wide, mobile portrait. `▣` = checked, `☐` = unchecked. **All names, vendors, dates and amounts are illustrative.** The real values come from the sheet and from `wedding-profile.json`.

### 9.1 Home (S-10)

```
┌──────────────────────────────────────┐
│ Nama CPW & Nama CPP   [☁ Tersinkron] │  top app bar + SyncPill
├──────────────────────────────────────┤
│ ╭──────────────────────────────────╮ │
│ │   H-        83                   │ │  primaryContainer hero, 28dp corners
│ │   hari lagi menuju hari bahagia  │ │  Fraunces numeral, gold accent
│ │   ‹hari›, ‹tanggal pernikahan›   │ │
│ │ ──────────────────────────────── │ │
│ │ ▸ Pengajian/Siraman · ‹tgl› (H-82)│
│ ╰──────────────────────────────────╯ │
│                                      │
│ Progress Pembayaran            ›     │
│ ╭──────────────────────────────────╮ │
│ │  ◯ 25,0%   Total   Rp120.000.000 │ │  ProgressRing 96dp
│ │  ring      Dibayar  Rp30.000.000 │ │
│ │            Sisa     Rp90.000.000 │ │
│ │  Item lunas: 12 dari 40          │ │
│ ╰──────────────────────────────────╯ │
│                                      │
│ Jatuh Tempo                Lihat semua│
│ ╭──────────────────────────────────╮ │
│ │ ▌12▐ Catering · Vendor A  3 hari │ │  red date block ≤3d
│ │ ▌DES▐ Sisa Rp20.000.000          │ │
│ │ ▌20▐ MUA + Pakaian       11 hari │ │  amber
│ │ ▌DES▐ Sisa Rp9.000.000           │ │
│ ╰──────────────────────────────────╯ │
│                                      │
│ Kesehatan Budget                     │
│ ╭──────────────────────────────────╮ │
│ │ Budget      Rp130.000.000        │ │
│ │ Estimasi    Rp120.000.000  92,3% │ │  linear bar
│ │ Selisih     +Rp10.000.000  ✓     │ │  becomes error card if negative
│ ╰──────────────────────────────────╯ │
│ (v1.1) Checklist  CPP 41% · CPW 40%  │
├──────────────────────────────────────┤
│  ♥Beranda  ▭Anggaran  ☑Checklist  👥Tamu  ⋯ │
└──────────────────────────────────────┘
```

### 9.2 Budget list (S-20)

```
┌──────────────────────────────────────┐
│ Anggaran                [☁ 1 menunggu]│
├──────────────────────────────────────┤
│ ( Akad & Resepsi )( Mahar & Seserahan)│  segmented
│ Total Rp100.000.000 · Sisa Rp75jt    │  summary strip
│ 🔍 Cari item atau vendor              │
│ [Kategori ▾] [Status ▾] [Urut: Jatuh tempo ▾]
├──────────────────────────────────────┤
│ 🍽 Catering                Rp20.000.000│
│    Vendor A · 250 pcs  ( ○ Belum bayar)│
│    ▱▱▱▱▱▱▱▱▱▱ 0%       ⏰ 12 Des      │
│──────────────────────────────────────│
│ 👗 MUA + Pakaian Pasangan Rp10.000.000 │
│    Vendor B · 1 paket       ( ◐ DP )  │
│    ▰▱▱▱▱▱▱▱▱▱ 10%  Sisa Rp9.000.000 ☁̸│  pending marker
│──────────────────────────────────────│
│ 📄 Dokumen                    Rp200.000│
│    1 paket                ( ✓ Lunas ) │
│──────────────────────────────────────│
│ 📄 Penghulu                 Rp2.000.000│
│    ⚠ Harga belum diisi …              │  (example of yellow-flag state)
│                                      │
│                         ╭──────────╮ │
│                         │ + Tambah │ │  extended FAB
│                         ╰──────────╯ │
└──────────────────────────────────────┘
```

### 9.3 Budget item detail (S-21) and payment sheet (S-23)

```
┌──────────────────────────────────────┐   ┌──────────────────────────────────────┐
│ ←  Catering                  ✎   ⋮   │   │ ───── (drag handle)                  │
├──────────────────────────────────────┤   │ Catat Pembayaran                     │
│ [Catering]            ( ○ Belum bayar)│   │ ( Tambah pembayaran )( Set total )   │
│                                      │   │ ┌──────────────────────────────────┐ │
│ Dibayar Rp0 dari Rp20.000.000        │   │ │ Rp  5.000.000                    │ │
│ ▱▱▱▱▱▱▱▱▱▱▱▱▱▱▱▱▱▱▱▱  0%             │   │ └──────────────────────────────────┘ │
│                                      │   │ [DP 30%] [50%] [Lunasi]              │
│ ┌─────────────┬────────────────────┐ │   │                                      │
│ │ Qty × Harga │ 250 pcs × Rp80.000 │ │   │ Setelah disimpan                     │
│ │ Total       │ Rp20.000.000       │ │   │ Dibayar  Rp5.000.000                 │
│ │ Sisa        │ Rp20.000.000       │ │   │ Sisa     Rp15.000.000   ( ◐ DP )     │
│ └─────────────┴────────────────────┘ │   │                                      │
│ ⏰ Jatuh Tempo   12 Des (H-47)        │   │ Jatuh tempo berikutnya  [12 Des]     │
│ 🏪 Vendor        Vendor A          ›  │   │ Catatan  [DP catering via transfer ] │
│ 📝 Catatan       —                    │   │                                      │
│                                      │   │ [ Batal ]          [■ Simpan ]       │
│ [ Lunasi ]   [■ Catat Pembayaran ]   │   └──────────────────────────────────────┘
└──────────────────────────────────────┘
```

**Edit item (S-22):** a full-screen form. Field order: Kategori (dropdown) · Detail Item · Vendor (Akad) or Dibayar oleh: (Groom | Bride) (Mahar) · Qty + Satuan (one row, 1:1) · Harga satuan (`RupiahField` + "Pakai rumus") · read-only card "Total Rp…" · Sudah Dibayar (`RupiahField`) · Jatuh Tempo (date picker) · Catatan (multiline). The bottom bar has Batal / Simpan. If Price holds a formula, the field shows `=1000000+500000+250000` with "= Rp1.750.000" under it.

### 9.4 Checklist (S-31)

```
┌──────────────────────────────────────┐
│ ←  Checklist Pernikahan  [☁ Tersinkron]│
│ (Semua)(Belum selesai (saya))(Selesai)│
├──────────────────────────────────────┤
│ 1 · Administrasi & Legalitas         │  sticky header
│ CPP ▰▰▰▰▰▰▰▰▰▱ 94%  CPW ▰▰▰▰▰▰▰▰▱▱ 78% │
│──────────────────────────────────────│
│ Surat Pengantar dari RT/RW    ▣    ▣ │
│ Diserahkan kepada Desa/Kel.  CPP  CPW│  detail as secondary text
│ Surat pengantar nikah (N1)    ▣    ☐ │  CPW column highlighted for the CPW user
│ Keterangan sudah Vaksin TT    ▣    ☐ │
│ Bimbingan Pra-Nikah KUA       ☐    ☐ │
│──────────────────────────────────────│
│ 2 · Persiapan Akad & Resepsi         │
│ ▾ Mahar & Seserahan   CPP 43% CPW 43%│  collapsible sub-section
│   Mahar                       ▣    ▣ │
│   Seserahan                   ☐    ☐ │
│ ▸ Venue                83% · 83%     │  collapsed
│ ▸ Attire & Makeup      78% · 78%     │
└──────────────────────────────────────┘
```

**Perawatan (S-31, single mode):** grouped by Waktu ("H-3 Bulan · sekitar ‹tanggal›") with single checkboxes. The date hint is computed from the wedding date.

### 9.5 Guest list (S-40)

```
┌──────────────────────────────────────┐
│ Tamu                     [☁ Tersinkron]│
│ (  CPP 150  )(  CPW 150  )( Semua )  │
│ 🔍 Cari nama                          │
│ [Kategori ▾][Hadir ▾][Digital ▾][Fisik ▾]│
├──────────────────────────────────────┤
│ Keluarga · a. Inti              (3)  │  group header
│ Ayah CPP                             │
│   [Digital: Tidak][Fisik: Tidak][Hadir: Ya]│ tappable chips
│ Ibu CPP                              │
│   [Digital: Tidak][Fisik: Tidak][Hadir: Ya]│
│──────────────────────────────────────│
│ Keluarga · Bapak                (12) │
│ Paman (pihak ayah)                   │
│   [Digital: Tidak][Fisik: Tidak][Hadir: Mungkin]│
│ …                                    │
│ ╭─ Ringkasan: Hadir Ya 120 · Mungkin 30 ─╮│  sticky footer → S-42
│                        ╭───────────╮ │
│                        │ + Tamu    │ │
└──────────────────────────────────────┘
```

### 9.6 Vendor comparison (S-51)

```
┌──────────────────────────────────────┐
│ ←  Perbandingan Vendor               │
│ [Jasa WO][Venue][Dekorasi][Catering][Foto…]│ scrollable tabs
├──────────────────────────────────────┤
│              │ WO A     │ WO B     │ WO C    │  → horizontal scroll
│              │ ★ Dipilih│          │         │  marks FinalVendor match
│──────────────┼──────────┼──────────┼─────────│
│ Harga        │ Rp6 jt   │ Rp5 jt   │ Rp7 jt  │
│ Jumlah crew  │ 7-8      │ 5        │ 5       │
│ Bride assist.│   ✓      │   ✗      │   ✓     │
│ Izin venue   │   ✗      │   ✗      │   ✓     │
│ Berkas KUA   │   ✗      │   ✗      │   ✓     │
│ …            │          │          │         │
│ Maks. acara  │ 3++      │ 7 jam    │ 10 jam  │
│ Catatan PLUS │ —        │ bonus dekor…       │
│ Catatan MINUS│ —        │ —        │ Tergan… │  tap cell → full text sheet
│ Tgl tersedia │   ✓      │   ✗      │   ✓     │
└──────────────────────────────────────┘
```

The first column is sticky at 120dp. Candidate columns are at least 96dp wide. Booleans use icon + colour: ✓ in Lunas green, ✗ in `onSurfaceVariant`.

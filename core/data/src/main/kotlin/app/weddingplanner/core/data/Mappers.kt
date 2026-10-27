package app.weddingplanner.core.data

import app.weddingplanner.core.database.AppUserEntity
import app.weddingplanner.core.database.BudgetItemEntity
import app.weddingplanner.core.database.BudgetSummaryEntity
import app.weddingplanner.core.database.FinalVendorEntity
import app.weddingplanner.core.database.WeddingEventEntity
import app.weddingplanner.core.model.AppUser
import app.weddingplanner.core.model.BudgetItem
import app.weddingplanner.core.model.BudgetSummary
import app.weddingplanner.core.model.BudgetTable
import app.weddingplanner.core.model.EventType
import app.weddingplanner.core.model.FinalVendor
import app.weddingplanner.core.model.Role
import app.weddingplanner.core.model.Side
import app.weddingplanner.core.model.WeddingEvent
import java.time.LocalDate

internal fun BudgetItem.toEntity() = BudgetItemEntity(
    row_id = rowId, budget_table = table.name, sheet_row = sheetRow, no = no, category_raw = categoryRaw,
    detail = detail, vendor_or_payer = vendorOrPayer, qty = qty, unit = unit, price = price,
    price_formula = priceFormula, total = total, paid = paid, remaining = remaining, progress = progress,
    status_raw = statusRaw, due_epoch_day = dueDate?.toEpochDay(), notes = notes,
)

internal fun BudgetItemEntity.toModel() = BudgetItem(
    rowId = row_id, table = BudgetTable.valueOf(budget_table), sheetRow = sheet_row, no = no,
    categoryRaw = category_raw, detail = detail, vendorOrPayer = vendor_or_payer, qty = qty, unit = unit,
    price = price, priceFormula = price_formula, total = total, paid = paid, remaining = remaining,
    progress = progress, statusRaw = status_raw, dueDate = due_epoch_day?.let(LocalDate::ofEpochDay), notes = notes,
)

internal fun BudgetSummary.toEntity() = BudgetSummaryEntity(
    budgeting_nikah = budgetingNikah, budget_terpakai = budgetTerpakai, total_estimasi = totalEstimasi,
    selisih = selisih, total_akad_resepsi = totalAkadResepsi, total_mahar_seserahan = totalMaharSeserahan,
    total_biaya = totalBiaya, sudah_dibayar = sudahDibayar, sisa = sisaPembayaran, progress = progress,
    item_lunas = itemLunas, item_total = itemTotal, due_14_days = dueIn14Days,
)

internal fun BudgetSummaryEntity.toModel() = BudgetSummary(
    budgetingNikah = budgeting_nikah, budgetTerpakai = budget_terpakai, totalEstimasi = total_estimasi,
    selisih = selisih, totalAkadResepsi = total_akad_resepsi, totalMaharSeserahan = total_mahar_seserahan,
    totalBiaya = total_biaya, sudahDibayar = sudah_dibayar, sisaPembayaran = sisa, progress = progress,
    itemLunas = item_lunas, itemTotal = item_total, dueIn14Days = due_14_days,
)

internal fun FinalVendor.toEntity() = FinalVendorEntity(category, sortOrder, vendorName, paymentRaw, meetingRaw, notes)
internal fun FinalVendorEntity.toModel() = FinalVendor(category, sheet_row, vendor_name, payment_raw, meeting_raw, notes)

internal fun WeddingEvent.toEntity() = WeddingEventEntity(type.name, date.toEpochDay())
internal fun WeddingEventEntity.toModel(): WeddingEvent? =
    EventType.entries.firstOrNull { it.name == type }?.let { WeddingEvent(it, LocalDate.ofEpochDay(epoch_day)) }

internal fun AppUser.toEntity() = AppUserEntity(email, side?.name, role.name)
internal fun AppUserEntity.toModel() = AppUser(email, side?.let(Side::valueOf), Role.valueOf(role))

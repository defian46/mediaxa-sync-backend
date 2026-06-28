package com.mediaxa.business.suite.data.remote.mapper

import com.mediaxa.business.suite.data.local.entity.*
import com.mediaxa.business.suite.data.remote.dto.*

// ─────────────────────────────────────────────────────────────────────────────
// Transaction Mappers
// ─────────────────────────────────────────────────────────────────────────────

fun Transaction.toDto() = TransactionDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    transactionNumber = transactionNumber,
    timestamp = timestamp,
    cashierUuid = cashierUuid,
    cashierName = cashierName,
    discount = discount,
    subtotal = subtotal,
    total = total,
    transactionHpp = transactionHpp,
    grossProfit = grossProfit,
    paymentMethod = paymentMethod,
    amountReceived = amountReceived,
    changeAmount = changeAmount,
    status = status,
    customerUuid = customerUuid,
    pointsEarned = pointsEarned,
    pointsRedeemed = pointsRedeemed,
    notes = notes,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    isDeleted = isDeleted
)

fun TransactionItem.toDto() = TransactionItemDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    transactionUuid = transactionUuid,
    menuUuid = menuUuid,
    menuName = menuName,
    quantity = quantity,
    unitPrice = price,
    subtotal = subtotal,
    hpp = costPrice,
    notes = null,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun Payment.toDto() = PaymentDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    transactionUuid = transactionUuid,
    method = method,
    amount = amount,
    referenceNumber = null,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

// ─────────────────────────────────────────────────────────────────────────────
// Customer & Loyalty Mappers
// ─────────────────────────────────────────────────────────────────────────────

fun Customer.toDto() = CustomerDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    customerCode = customerCode,
    customerName = customerName,
    phone = phone,
    email = email,
    birthday = birthday,
    gender = gender,
    address = address,
    notes = notes,
    joinDate = joinDate,
    membershipLevel = membershipLevel,
    totalSpending = totalSpending,
    lastVisit = lastVisit,
    favoriteMenuUuid = favoriteMenuUuid,
    updatedAt = updatedAt,
    isDeleted = isDeleted != 0  // Int → Boolean
)

fun LoyaltyPointHistory.toDto() = LoyaltyPointHistoryDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    customerUuid = customerUuid,
    transactionUuid = transactionUuid,
    points = points,
    activityType = activityType,
    expiryTime = expiryTime,
    notes = notes,
    updatedAt = updatedAt,
    isDeleted = isDeleted != 0  // Int → Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
// Menu & Category Mappers
// ─────────────────────────────────────────────────────────────────────────────

fun Menu.toDto() = MenuDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    name = name,
    categoryUuid = categoryUuid,
    price = price,
    promoPrice = promoPrice,
    isActive = isActive,
    description = description,
    estimatedHpp = estimatedHpp,
    estimatedMargin = estimatedMargin,
    imageUrl = imagePath,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun Category.toDto() = CategoryDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    name = name,
    displayOrder = displayOrder,
    isActive = isActive,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

// ─────────────────────────────────────────────────────────────────────────────
// Inventory Mappers
// ─────────────────────────────────────────────────────────────────────────────

fun Ingredient.toDto() = IngredientDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    name = name,
    unit = unit,
    purchasePrice = purchasePrice,
    packageSize = packageSize,
    availableStock = availableStock,
    minStock = minStock,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun MenuRecipe.toDto() = MenuRecipeDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    menuUuid = menuUuid,
    ingredientUuid = ingredientUuid,
    quantityNeeded = quantityNeeded,
    unit = "unit", // MenuRecipe does not store unit separately; defaulted
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun StockMovement.toDto() = StockMovementDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    ingredientUuid = ingredientUuid,
    movementType = type,
    quantity = quantity,
    referenceUuid = null,
    referenceType = null,
    notes = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ─────────────────────────────────────────────────────────────────────────────
// Purchase Expense Mappers
// ─────────────────────────────────────────────────────────────────────────────

fun PurchaseExpense.toDto() = PurchaseExpenseDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    purchaseDate = purchaseDate,
    purchasePlaceName = purchasePlaceName,
    paymentMethod = paymentMethod,
    notes = notes,
    totalAmount = totalAmount,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun PurchaseExpenseItem.toDto() = PurchaseExpenseItemDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    purchaseExpenseUuid = purchaseExpenseUuid,
    ingredientUuid = ingredientUuid,
    quantity = quantity,
    unit = unit,
    totalPrice = totalPrice,
    unitPrice = unitPrice,
    batchNumber = batchNumber,
    expiredDate = expiredDate,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

// ─────────────────────────────────────────────────────────────────────────────
// Stock Opname Mappers
// ─────────────────────────────────────────────────────────────────────────────

fun StockOpname.toDto() = StockOpnameDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    opnameDate = opnameDate,
    userUuid = userUuid,
    notes = notes,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun StockOpnameItem.toDto() = StockOpnameItemDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    opnameUuid = opnameUuid,
    ingredientUuid = ingredientUuid,
    systemStock = systemStock,
    physicalStock = physicalStock,
    diffStock = diffStock,
    notes = notes,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

// ─────────────────────────────────────────────────────────────────────────────
// Waste Log Mapper
// ─────────────────────────────────────────────────────────────────────────────

fun WasteLog.toDto() = WasteLogDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    wasteDate = wasteDate,
    ingredientUuid = ingredientUuid,
    quantity = quantity,
    reason = reason,
    calculatedCost = calculatedCost,
    userUuid = userUuid,
    notes = notes,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

// ─────────────────────────────────────────────────────────────────────────────
// Expense Mapper
// ─────────────────────────────────────────────────────────────────────────────

fun Expense.toDto() = ExpenseDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    expenseDate = expenseDate,
    category = category,
    amount = amount,
    notes = notes,
    userUuid = userUuid,
    paymentMethod = paymentMethod,
    attachmentPath = attachmentPath,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

// ─────────────────────────────────────────────────────────────────────────────
// Promotion Mapper
// ─────────────────────────────────────────────────────────────────────────────

fun PromotionRule.toDto() = PromotionRuleDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    name = name,
    promoType = promoType,
    value = value,
    buyMenuUuid = buyMenuUuid,
    buyQuantity = buyQuantity,
    getMenuUuid = getMenuUuid,
    getQuantity = getQuantity,
    minPurchaseAmount = minPurchaseAmount,
    isActive = isActive,
    startDate = startDate,
    endDate = endDate,
    startHour = startHour,
    endHour = endHour,
    applicableDays = applicableDays,
    targetMembershipLevels = targetMembershipLevels,
    targetCategoryUuid = targetCategoryUuid,
    targetMenuUuid = targetMenuUuid,
    promoCode = promoCode,
    updatedAt = updatedAt,
    isDeleted = isDeleted != 0  // Int → Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
// Finance Mappers
// ─────────────────────────────────────────────────────────────────────────────

fun CashShift.toDto() = CashShiftDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    cashierUuid = cashierUuid,
    startTime = startTime,
    endTime = endTime,
    openingCash = openingCash,
    closingCash = closingCash,
    expectedCash = expectedCash,
    actualCash = actualCash,
    cashDifference = cashDifference,
    status = status,
    updatedAt = updatedAt,
    isDeleted = isDeleted != 0  // Int → Boolean
)

fun DailyClosing.toDto() = DailyClosingDto(
    uuid = uuid,
    storeId = storeId,
    deviceId = deviceId,
    dateStr = dateStr,
    openingBalance = openingBalance,
    revenue = revenue,
    hpp = hpp,
    grossProfit = grossProfit,
    operationalExpense = operationalExpense,
    wasteCost = wasteCost,
    netProfit = netProfit,
    cashInflow = cashInflow,
    cashOutflow = cashOutflow,
    closingBalance = closingBalance,
    cashRevenue = cashRevenue,
    qrisRevenue = qrisRevenue,
    transferRevenue = transferRevenue,
    totalTransactions = totalTransactions,
    averageTicket = averageTicket,
    closedByUserUuid = closedByUserUuid,
    updatedAt = updatedAt,
    isDeleted = isDeleted != 0  // Int → Boolean
)

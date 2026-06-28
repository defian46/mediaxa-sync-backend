package com.mediaxa.business.suite.data.printing

import com.mediaxa.business.suite.data.local.entity.Transaction
import com.mediaxa.business.suite.data.local.entity.TransactionItem
import com.mediaxa.business.suite.data.local.entity.StoreSetting
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

object EscPosFormatter {

    private val rpFormatter = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun formatReceiptText(
        transaction: Transaction,
        items: List<TransactionItem>,
        settings: StoreSetting,
        lineWidth: Int = 32
    ): String {
        val sb = StringBuilder()

        // 1. Header
        sb.append(centerAlign(settings.storeName.uppercase(), lineWidth)).append("\n")
        if (!settings.address.isNullOrEmpty()) {
            sb.append(centerAlign(settings.address, lineWidth)).append("\n")
        }
        if (!settings.phoneNumber.isNullOrEmpty()) {
            sb.append(centerAlign("Telp: ${settings.phoneNumber}", lineWidth)).append("\n")
        }
        sb.append("-".repeat(lineWidth)).append("\n")

        // 2. Metadata
        val dateStr = dateFormat.format(transaction.timestamp)
        sb.append(justifyLeftRight("Tgl: $dateStr", "", lineWidth)).append("\n")
        sb.append(justifyLeftRight("No : ${transaction.transactionNumber}", "", lineWidth)).append("\n")
        sb.append(justifyLeftRight("Ksr: ${transaction.cashierName}", "", lineWidth)).append("\n")
        sb.append("-".repeat(lineWidth)).append("\n")

        // 3. Items
        for (item in items) {
            sb.append(item.menuName).append("\n")
            val priceQtyStr = "  ${item.quantity} x ${rpFormatter.format(item.price)}"
            val totalStr = rpFormatter.format(item.subtotal)
            sb.append(justifyLeftRight(priceQtyStr, totalStr, lineWidth)).append("\n")
        }
        sb.append("-".repeat(lineWidth)).append("\n")

        // 4. Totals
        sb.append(justifyLeftRight("Subtotal", rpFormatter.format(transaction.subtotal), lineWidth)).append("\n")
        if (transaction.discount > 0) {
            sb.append(justifyLeftRight("Diskon", "-${rpFormatter.format(transaction.discount)}", lineWidth)).append("\n")
        }
        sb.append(justifyLeftRight("TOTAL", rpFormatter.format(transaction.total), lineWidth)).append("\n")
        sb.append(justifyLeftRight("Bayar", rpFormatter.format(transaction.amountReceived), lineWidth)).append("\n")
        sb.append(justifyLeftRight("Kembali", rpFormatter.format(transaction.changeAmount), lineWidth)).append("\n")

        if (transaction.paymentMethod == "TRANSFER") {
            sb.append("-".repeat(lineWidth)).append("\n")
            sb.append(centerAlign("Transfer Ke:", lineWidth)).append("\n")
            sb.append(centerAlign("${settings.bankName ?: "-"} - ${settings.bankAccountNumber ?: "-"}", lineWidth)).append("\n")
            sb.append(centerAlign("A/N: ${settings.bankAccountHolderName ?: "-"}", lineWidth)).append("\n")
        }
        
        // 5. Footer
        sb.append("-".repeat(lineWidth)).append("\n")
        if (!settings.receiptFooter.isNullOrEmpty()) {
            sb.append(centerAlign(settings.receiptFooter, lineWidth)).append("\n")
        }
        sb.append(centerAlign("Powered by Mediaxa", lineWidth)).append("\n")

        return sb.toString()
    }

    fun formatEscPosBytes(
        transaction: Transaction,
        items: List<TransactionItem>,
        settings: StoreSetting
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        // ESC/POS Command Constants
        val ESC: Byte = 0x1B
        val GS: Byte = 0x1D

        // Initialize Printer: ESC @
        bytes.addAll(listOf(ESC, 0x40))

        // Center Align: ESC a 1
        bytes.addAll(listOf(ESC, 0x61, 0x01))
        // Title double-size: GS ! 0x11 (double width, double height)
        bytes.addAll(listOf(GS, 0x21, 0x11))
        bytes.addAll(settings.storeName.uppercase().toByteArray().toList())
        bytes.addAll(listOf(0x0A.toByte())) // newline

        // Reset Text Size: GS ! 0x00
        bytes.addAll(listOf(GS, 0x21, 0x00))

        // Address & Phone
        if (!settings.address.isNullOrEmpty()) {
            bytes.addAll(settings.address.toByteArray().toList())
            bytes.addAll(listOf(0x0A.toByte()))
        }
        if (!settings.phoneNumber.isNullOrEmpty()) {
            bytes.addAll("Telp: ${settings.phoneNumber}".toByteArray().toList())
            bytes.addAll(listOf(0x0A.toByte()))
        }

        // Left Align: ESC a 0
        bytes.addAll(listOf(ESC, 0x61, 0x00))
        bytes.addAll("-".repeat(32).toByteArray().toList())
        bytes.addAll(listOf(0x0A.toByte()))

        // Metadata
        val dateStr = dateFormat.format(transaction.timestamp)
        bytes.addAll("Tgl: $dateStr\n".toByteArray().toList())
        bytes.addAll("No : ${transaction.transactionNumber}\n".toByteArray().toList())
        bytes.addAll("Ksr: ${transaction.cashierName}\n".toByteArray().toList())
        bytes.addAll("-".repeat(32).toByteArray().toList())
        bytes.addAll(listOf(0x0A.toByte()))

        // Items
        for (item in items) {
            bytes.addAll("${item.menuName}\n".toByteArray().toList())
            val line = justifyLeftRight("  ${item.quantity} x ${rpFormatter.format(item.price)}", rpFormatter.format(item.subtotal), 32)
            bytes.addAll(line.toByteArray().toList())
            bytes.addAll(listOf(0x0A.toByte()))
        }
        bytes.addAll("-".repeat(32).toByteArray().toList())
        bytes.addAll(listOf(0x0A.toByte()))

        // Totals
        bytes.addAll(justifyLeftRight("Subtotal", rpFormatter.format(transaction.subtotal), 32).toByteArray().toList())
        bytes.addAll(listOf(0x0A.toByte()))
        if (transaction.discount > 0) {
            bytes.addAll(justifyLeftRight("Diskon", "-${rpFormatter.format(transaction.discount)}", 32).toByteArray().toList())
            bytes.addAll(listOf(0x0A.toByte()))
        }
        // Total bold: ESC E 1
        bytes.addAll(listOf(ESC, 0x45, 0x01))
        bytes.addAll(justifyLeftRight("TOTAL", rpFormatter.format(transaction.total), 32).toByteArray().toList())
        bytes.addAll(listOf(0x0A.toByte()))
        // Bold off: ESC E 0
        bytes.addAll(listOf(ESC, 0x45, 0x00))

        bytes.addAll(justifyLeftRight("Bayar", rpFormatter.format(transaction.amountReceived), 32).toByteArray().toList())
        bytes.addAll(listOf(0x0A.toByte()))
        bytes.addAll(justifyLeftRight("Kembali", rpFormatter.format(transaction.changeAmount), 32).toByteArray().toList())
        bytes.addAll(listOf(0x0A.toByte()))

        if (transaction.paymentMethod == "TRANSFER") {
            bytes.addAll("-".repeat(32).toByteArray().toList())
            bytes.addAll(listOf(0x0A.toByte()))
            bytes.addAll(listOf(ESC, 0x61, 0x01))
            bytes.addAll("Transfer Ke:\n".toByteArray().toList())
            bytes.addAll("${settings.bankName ?: "-"} - ${settings.bankAccountNumber ?: "-"}\n".toByteArray().toList())
            bytes.addAll("A/N: ${settings.bankAccountHolderName ?: "-"}\n".toByteArray().toList())
            bytes.addAll(listOf(ESC, 0x61, 0x00))
        }

        // Footer
        bytes.addAll("-".repeat(32).toByteArray().toList())
        bytes.addAll(listOf(0x0A.toByte()))
        // Center Align: ESC a 1
        bytes.addAll(listOf(ESC, 0x61, 0x01))
        if (!settings.receiptFooter.isNullOrEmpty()) {
            bytes.addAll(settings.receiptFooter.toByteArray().toList())
            bytes.addAll(listOf(0x0A.toByte()))
        }
        bytes.addAll("Powered by Mediaxa\n\n\n\n".toByteArray().toList())

        // Cut paper command: GS V 66 0
        bytes.addAll(listOf(GS, 0x56, 0x42, 0x00))

        return bytes.toByteArray()
    }

    private fun centerAlign(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text
    }

    private fun justifyLeftRight(left: String, right: String, width: Int): String {
        val leftLen = left.length
        val rightLen = right.length
        if (leftLen + rightLen >= width) {
            val available = width - rightLen - 1
            val truncatedLeft = if (available > 0) left.take(available) else ""
            return truncatedLeft + " " + right
        }
        val spaces = width - leftLen - rightLen
        return left + " ".repeat(spaces) + right
    }
}

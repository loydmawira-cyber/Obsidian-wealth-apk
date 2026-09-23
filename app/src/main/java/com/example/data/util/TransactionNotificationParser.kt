package com.example.data.util

import com.example.data.models.Category
import com.example.data.models.TransactionType
import java.security.MessageDigest
import java.util.Locale

data class ParsedNotificationTransaction(
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val reference: String,
    val dateMillis: Long
)

object TransactionNotificationParser {
    private val amountPattern = Regex("(?i)(?:[A-Z]{3}\\s*)?([0-9][0-9,]*(?:\\.[0-9]{1,2})?)")
    private val referencePattern = Regex("(?i)(?:ref(?:erence)?|txn|transaction|receipt|code)[:\\s#-]*([A-Z0-9-]{5,})")
    private val incomeWords = listOf("credited", "received", "deposit", "salary", "payment received", "inflow", "added")
    private val expenseWords = listOf("paid", "purchase", "withdrawn", "debit", "sent", "transfer to", "payment to", "spent")
    private val ignoredWords = listOf("otp", "one-time password", "verification code", "login code", "security code", "pin")

    fun parse(packageName: String, notificationTitle: String?, notificationText: String?, timestamp: Long): ParsedNotificationTransaction? {
        val raw = listOf(notificationTitle.orEmpty(), notificationText.orEmpty()).joinToString(" ").trim()
        if (raw.isBlank() || ignoredWords.any { raw.contains(it, ignoreCase = true) }) return null
        val amount = amountPattern.find(raw)?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null
        val lower = raw.lowercase(Locale.ROOT)
        val type = when {
            incomeWords.any { lower.contains(it) } -> TransactionType.INCOME
            expenseWords.any { lower.contains(it) } -> TransactionType.EXPENSE
            else -> return null
        }
        val reference = referencePattern.find(raw)?.groupValues?.getOrNull(1) ?: fingerprint(packageName, raw, timestamp)
        val category = when {
            type == TransactionType.INCOME && lower.contains("salary") -> Category.SALARY
            type == TransactionType.INCOME && lower.contains("dividend") -> Category.DIVIDENDS
            type == TransactionType.EXPENSE && listOf("rent", "mortgage", "housing").any { lower.contains(it) } -> Category.HOUSING
            type == TransactionType.EXPENSE && listOf("electric", "power", "water", "internet", "utility").any { lower.contains(it) } -> Category.UTILITIES
            type == TransactionType.EXPENSE && listOf("food", "restaurant", "grocery", "supermarket").any { lower.contains(it) } -> Category.FOOD_DINING
            type == TransactionType.EXPENSE && listOf("transport", "fuel", "uber", "taxi").any { lower.contains(it) } -> Category.TRANSPORT
            else -> Category.OTHER
        }
        val title = notificationTitle?.takeIf { it.isNotBlank() } ?: if (type == TransactionType.INCOME) "Imported income" else "Imported payment"
        return ParsedNotificationTransaction(title, amount, type, category, reference, timestamp)
    }

    private fun fingerprint(packageName: String, text: String, timestamp: Long): String {
        val day = timestamp / 86_400_000L
        val input = "$packageName|${text.trim().lowercase(Locale.ROOT)}|$day"
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

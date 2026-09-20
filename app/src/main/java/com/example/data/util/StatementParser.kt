package com.example.data.util

import com.example.data.models.Category
import com.example.data.models.TransactionType
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParsedTransaction(
    val dateMillis: Long,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val account: String,
    val fingerprint: String,
    val isDuplicate: Boolean = false,
    val dateAmbiguous: Boolean = false
)

object StatementParser {

    fun generateFingerprint(dateMillis: Long, amount: Double, title: String, type: TransactionType): String {
        val raw = "$dateMillis|${String.format(Locale.US, "%.2f", amount)}|${title.trim().lowercase(Locale.ROOT)}|${type.name}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun parseStatementText(rawText: String, defaultAccount: String = "Bank Import"): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()
        if (rawText.isBlank()) return results

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            val parsed = parseCsvLine(line, defaultAccount)
                ?: parseMPesaSmsLine(line, defaultAccount)
                ?: parseGenericLine(line, defaultAccount)
            if (parsed != null) {
                results.add(parsed)
            }
        }
        return results
    }

    private fun parseCsvLine(line: String, defaultAccount: String): ParsedTransaction? {
        if (!line.contains(",")) return null
        val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
        if (parts.size < 3) return null

        // Try date in part 0, description in part 1, amount in part 2
        val parsedDate = parseDateString(parts[0])
        val dateMillis = parsedDate ?: 0L
        val dateAmbiguous = (parsedDate == null)

        val title = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
        val rawAmount = parts.getOrNull(2)?.toDoubleOrNull() ?: return null

        val amount = Math.abs(rawAmount)
        val type = if (rawAmount < 0 || line.contains("DEBIT", ignoreCase = true)) TransactionType.EXPENSE else TransactionType.INCOME
        val category = inferCategory(title)
        val account = parts.getOrNull(3)?.takeIf { it.isNotBlank() } ?: defaultAccount
        val fp = generateFingerprint(dateMillis, amount, title, type)

        return ParsedTransaction(
            dateMillis = dateMillis,
            title = title,
            amount = amount,
            type = type,
            category = category,
            account = account,
            fingerprint = fp,
            dateAmbiguous = dateAmbiguous
        )
    }

    private fun parseMPesaSmsLine(line: String, defaultAccount: String): ParsedTransaction? {
        if (!line.contains("M-PESA", ignoreCase = true) && !line.contains("Ksh", ignoreCase = true)) return null

        val amountRegex = Regex("Ksh\\s*([\\d,]+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(line) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null

        val isIncome = line.contains("received", ignoreCase = true)
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        val title = when {
            line.contains("paid to", ignoreCase = true) -> {
                line.substringAfter("paid to", "").substringBefore(" on ", "").trim()
            }
            line.contains("from", ignoreCase = true) -> {
                line.substringAfter("from", "").substringBefore(" on ", "").trim()
            }
            else -> "M-Pesa Transaction"
        }.ifBlank { "M-Pesa Record" }

        val parsedDate = parseDateString(line)
        val dateMillis = parsedDate ?: 0L
        val dateAmbiguous = (parsedDate == null)

        val category = inferCategory(title)
        val fp = generateFingerprint(dateMillis, amount, title, type)

        return ParsedTransaction(
            dateMillis = dateMillis,
            title = title,
            amount = amount,
            type = type,
            category = category,
            account = "M-Pesa Wallet",
            fingerprint = fp,
            dateAmbiguous = dateAmbiguous
        )
    }

    private fun parseGenericLine(line: String, defaultAccount: String): ParsedTransaction? {
        val numberRegex = Regex("([+\\-]?\\d+(?:,\\d{3})*(?:\\.\\d+)?)")
        val numbers = numberRegex.findAll(line).mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }.toList()
        if (numbers.isEmpty()) return null

        val rawAmount = numbers.last()
        val amount = Math.abs(rawAmount)
        if (amount == 0.0) return null

        val parsedDate = parseDateString(line)
        val dateMillis = parsedDate ?: 0L
        val dateAmbiguous = (parsedDate == null)

        val title = line.take(40).trim()
        val type = if (rawAmount < 0 || line.contains("dr", ignoreCase = true) || line.contains("debit", ignoreCase = true)) {
            TransactionType.EXPENSE
        } else {
            TransactionType.INCOME
        }

        val category = inferCategory(title)
        val fp = generateFingerprint(dateMillis, amount, title, type)

        return ParsedTransaction(
            dateMillis = dateMillis,
            title = title,
            amount = amount,
            type = type,
            category = category,
            account = defaultAccount,
            fingerprint = fp,
            dateAmbiguous = dateAmbiguous
        )
    }

    fun parseDateString(str: String): Long? {
        val patterns = listOf(
            Regex("""\b(\d{4}[-/]\d{1,2}[-/]\d{1,2})\b"""),
            Regex("""\b(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})\b"""),
            Regex("""\b(\d{1,2}[-\s]+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[-\s]+\d{2,4})\b""", RegexOption.IGNORE_CASE)
        )

        val formats = listOf(
            "yyyy-MM-dd", "yyyy/MM/dd",
            "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy",
            "dd/MM/yy", "MM/dd/yy", "dd-MM-yy",
            "d/M/yy", "d/M/yyyy",
            "dd MMM yyyy", "dd MMMM yyyy",
            "dd-MMM-yyyy", "dd-MMMM-yyyy",
            "d-MMM-yyyy", "d-MMMM-yyyy",
            "d MMM yyyy", "d MMMM yyyy"
        )

        // 1. Direct try on trimmed string
        val trimmed = str.trim()
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }
                val d = sdf.parse(trimmed)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }

        // 2. Pattern matching within substrings
        for (pattern in patterns) {
            val match = pattern.find(str) ?: continue
            val candidate = match.groupValues[1]
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }
                    val d = sdf.parse(candidate)
                    if (d != null) return d.time
                } catch (_: Exception) {}
            }
        }

        return null
    }

    fun inferCategory(title: String): Category {
        val lower = title.lowercase(Locale.ROOT)
        return when {
            lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("kfc") || lower.contains("java") -> Category.FOOD_DINING
            lower.contains("fuel") || lower.contains("uber") || lower.contains("bolt") || lower.contains("transport") || lower.contains("matatu") -> Category.TRANSPORT
            lower.contains("kplc") || lower.contains("power") || lower.contains("water") || lower.contains("utility") || lower.contains("internet") || lower.contains("safaricom") -> Category.UTILITIES
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("subscription") -> Category.SUBSCRIPTIONS
            lower.contains("rent") || lower.contains("housing") || lower.contains("mortgage") -> Category.HOUSING
            lower.contains("hospital") || lower.contains("pharmacy") || lower.contains("health") -> Category.HEALTHCARE
            lower.contains("sip") || lower.contains("mmf") || lower.contains("stock") || lower.contains("invest") -> Category.INVESTMENT_SIP
            lower.contains("shop") || lower.contains("supermarket") || lower.contains("mall") || lower.contains("amazon") -> Category.SHOPPING
            else -> Category.OTHER
        }
    }
}

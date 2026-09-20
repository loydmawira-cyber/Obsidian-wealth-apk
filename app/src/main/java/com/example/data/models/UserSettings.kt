package com.example.data.models

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class SupportedCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val flag: String
) {
    USD("USD", "$", "US Dollar ($)", "🇺🇸"),
    KES("KES", "KSh ", "Kenyan Shilling (KSh)", "🇰🇪"),
    EUR("EUR", "€", "Euro (€)", "🇪🇺"),
    GBP("GBP", "£", "British Pound (£)", "🇬🇧"),
    INR("INR", "₹", "Indian Rupee (₹)", "🇮🇳"),
    CAD("CAD", "CA$", "Canadian Dollar (CA$)", "🇨🇦"),
    AUD("AUD", "A$", "Australian Dollar (A$)", "🇦🇺"),
    JPY("JPY", "¥", "Japanese Yen (¥)", "🇯🇵"),
    ZAR("ZAR", "R ", "South African Rand (R)", "🇿🇦"),
    AED("AED", "AED ", "UAE Dirham (AED)", "🇦🇪"),
    SGD("SGD", "S$", "Singapore Dollar (S$)", "🇸🇬"),
    MXN("MXN", "Mex$", "Mexican Peso (Mex$)", "🇲🇽"),
    ARS("ARS", "AR$", "Argentine Peso (AR$)", "🇦🇷")
}

enum class GeographicRegion(
    val id: String,
    val title: String,
    val defaultCurrency: SupportedCurrency,
    val defaultFiscal: FiscalCalendar,
    val defaultNumberStyle: NumberFormatStyle
) {
    NORTH_AMERICA("na", "North America (US & Canada)", SupportedCurrency.USD, FiscalCalendar.CALENDAR_YEAR, NumberFormatStyle.WESTERN_STANDARD),
    LATIN_AMERICA("latam", "Latin America (Mexico, Argentina & LATAM)", SupportedCurrency.MXN, FiscalCalendar.CALENDAR_YEAR, NumberFormatStyle.WESTERN_STANDARD),
    EUROPE("eu", "European Union & EEA", SupportedCurrency.EUR, FiscalCalendar.CALENDAR_YEAR, NumberFormatStyle.CONTINENTAL),
    UK("uk", "United Kingdom", SupportedCurrency.GBP, FiscalCalendar.UK_COMMONWEALTH, NumberFormatStyle.WESTERN_STANDARD),
    INDIA("in", "India & South Asia", SupportedCurrency.INR, FiscalCalendar.UK_COMMONWEALTH, NumberFormatStyle.SOUTH_ASIAN),
    EAST_AFRICA("ea", "Kenya & East Africa (EAC)", SupportedCurrency.KES, FiscalCalendar.MID_YEAR, NumberFormatStyle.WESTERN_STANDARD),
    AUSTRALIA("au", "Australia & Pacific", SupportedCurrency.AUD, FiscalCalendar.MID_YEAR, NumberFormatStyle.WESTERN_STANDARD),
    MIDDLE_EAST("me", "Middle East & GCC", SupportedCurrency.AED, FiscalCalendar.CALENDAR_YEAR, NumberFormatStyle.WESTERN_STANDARD),
    SOUTH_AFRICA("za", "South Africa", SupportedCurrency.ZAR, FiscalCalendar.UK_COMMONWEALTH, NumberFormatStyle.WESTERN_STANDARD)
}

enum class FiscalCalendar(
    val title: String,
    val subtitle: String
) {
    CALENDAR_YEAR("Calendar Year (Jan - Dec)", "Standard global accounting period"),
    UK_COMMONWEALTH("Fiscal Year (Apr - Mar)", "UK, India & Commonwealth standards"),
    MID_YEAR("Mid-Year (Jul - Jun)", "East Africa, Australia & Governmental")
}

enum class NumberFormatStyle(
    val title: String,
    val example: String
) {
    WESTERN_STANDARD("Standard (1,000,000.00)", "Millions & Billions notation"),
    SOUTH_ASIAN("South Asian (10,00,000.00)", "Lakhs & Crores notation"),
    CONTINENTAL("Continental (1.000.000,00)", "Dot thousand delimiter with comma decimal")
}

enum class AiRiskProfile(
    val title: String,
    val subtitle: String
) {
    AGGRESSIVE_ALPHA("Aggressive Growth", "Equity & alpha focused, higher volatility"),
    BALANCED_INSTITUTIONAL("Balanced Institutional", "Target 60/40, multi-asset risk parity"),
    CONSERVATIVE_PRESERVATION("Capital Preservation", "Debt clearance, capital retention & yield")
}

data class UserSettings(
    // Geographical & Regional
    val currency: SupportedCurrency = SupportedCurrency.KES,
    val region: GeographicRegion = GeographicRegion.EAST_AFRICA,
    val fiscalCalendar: FiscalCalendar = FiscalCalendar.MID_YEAR,
    val numberFormatStyle: NumberFormatStyle = NumberFormatStyle.WESTERN_STANDARD,

    // Module / Feature Preferences ("What we need and don't need")
    val enableCashFlow: Boolean = true,
    val enableInvestments: Boolean = true,
    val enableDebtCenter: Boolean = true,
    val enableGoals: Boolean = true,

    // AI & Floating Controls
    val showFloatingAiAdvisor: Boolean = true,
    val aiRiskProfile: AiRiskProfile = AiRiskProfile.BALANCED_INSTITUTIONAL,

    // Privacy & Security
    val hideBalances: Boolean = false,
    val biometricProtection: Boolean = true,

    // Push Notifications & Reminders
    val enableNotifications: Boolean = true,
    val enableBillDueReminders: Boolean = true,
    val enableSipReminders: Boolean = true,
    val enableDailyBriefingReminders: Boolean = true
) {
    fun formatAmount(amount: Double, forceVisible: Boolean = false): String {
        if (hideBalances && !forceVisible) {
            return "••••••"
        }

        val symbols = DecimalFormatSymbols(Locale.US)
        val formattedNumber = when (numberFormatStyle) {
            NumberFormatStyle.CONTINENTAL -> {
                symbols.groupingSeparator = '.'
                symbols.decimalSeparator = ','
                DecimalFormat("#,##0.00", symbols).format(amount)
            }
            NumberFormatStyle.SOUTH_ASIAN -> {
                // Indian numbering format (e.g. 12,34,567.89)
                formatIndianNumber(amount)
            }
            NumberFormatStyle.WESTERN_STANDARD -> {
                symbols.groupingSeparator = ','
                symbols.decimalSeparator = '.'
                DecimalFormat("#,##0.00", symbols).format(amount)
            }
        }

        return "${currency.symbol}$formattedNumber"
    }

    fun formatCompact(amount: Double, forceVisible: Boolean = false): String {
        if (hideBalances && !forceVisible) {
            return "••••"
        }

        val absVal = Math.abs(amount)
        val sign = if (amount < 0) "-" else ""

        val formattedValue = when {
            numberFormatStyle == NumberFormatStyle.SOUTH_ASIAN -> {
                when {
                    absVal >= 10_000_000 -> "${sign}${"%.1f".format(absVal / 10_000_000)} Cr"
                    absVal >= 100_000 -> "${sign}${"%.1f".format(absVal / 100_000)} L"
                    absVal >= 1_000 -> "${sign}${"%.1f".format(absVal / 1_000)} K"
                    else -> "${sign}${"%.0f".format(absVal)}"
                }
            }
            else -> {
                when {
                    absVal >= 1_000_000_000 -> "${sign}${"%.1f".format(absVal / 1_000_000_000)}B"
                    absVal >= 1_000_000 -> "${sign}${"%.1f".format(absVal / 1_000_000)}M"
                    absVal >= 1_000 -> "${sign}${"%.1f".format(absVal / 1_000)}K"
                    else -> "${sign}${"%.0f".format(absVal)}"
                }
            }
        }

        return "${currency.symbol}$formattedValue"
    }

    private fun formatIndianNumber(amount: Double): String {
        val isNegative = amount < 0
        val absVal = Math.abs(amount)
        val longPart = absVal.toLong()
        val decimalPart = String.format(Locale.US, "%.2f", absVal - longPart).substring(1) // ".XX"

        val str = longPart.toString()
        if (str.length <= 3) {
            return "${if (isNegative) "-" else ""}$str$decimalPart"
        }

        val last3 = str.takeLast(3)
        val rest = str.dropLast(3)
        val groups = mutableListOf<String>()
        var i = rest.length
        while (i > 0) {
            val start = Math.max(0, i - 2)
            groups.add(0, rest.substring(start, i))
            i -= 2
        }
        groups.add(last3)
        val formatted = groups.joinToString(",")
        return "${if (isNegative) "-" else ""}$formatted$decimalPart"
    }
}

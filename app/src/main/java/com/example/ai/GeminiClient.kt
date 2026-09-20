package com.example.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Advisor access to Gemini.
 *
 * Security: calls are routed through Firebase AI Logic, NOT the raw
 * generativelanguage.googleapis.com endpoint. There is deliberately no API key in this class
 * and no GEMINI_API_KEY read from BuildConfig — any key compiled into an APK is extractable and
 * therefore public. Firebase AI Logic holds the credential server-side and is gated by App Check.
 *
 * Honesty contract: this object must NEVER state a financial figure that did not arrive in an
 * [AdvisorSnapshot] built from the user's own records. When the model is unreachable, the
 * fallback is a deterministic summary computed from that snapshot, clearly labelled as offline.
 * It does not invent balances, rates, instruments, dates or scores.
 */
object GeminiClient {

    /**
     * Valid Gemini Flash model id. The previous value ("gemini-3.5-flash") does not exist, so
     * every live call 404'd and silently fell through to the fallback path.
     */
    private const val MODEL_NAME = "gemini-2.5-flash"

    private const val SYSTEM_INSTRUCTION = """
You are "Obsidian AI", a wealth management and financial intelligence assistant embedded in the Obsidian Wealth personal finance app.

Core Guidelines:
1. Grounding: Use ONLY the figures supplied in the "User Financial Context" block. Never invent, estimate or illustrate a balance, holding, interest rate, institution name, product name or date that is not in that block. If the context marks a section as having no recorded data, say that the data is not recorded yet and explain what the user should add — do not supply an example figure in its place.
2. Precision: Where the context provides numbers, compute honestly from them (savings rate, DTI, payoff ordering). Show your arithmetic inputs so the user can check them. If a metric cannot be computed from the supplied data, say which input is missing.
3. Tone: Professional, concise, data-driven, free of fluff. Encouraging but never salesy.
4. No product recommendations by name unless that instrument already appears in the user's context.
5. Safety: Include this disclosure when discussing any adjustment: "Obsidian AI provides data analysis and quantitative modelling, not certified personal financial advice."
6. Format: Executive Summary, Key Breakdown, Strategic Recommendations.
"""

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.2f
                topP = 0.95f
            },
            systemInstruction = content { text(SYSTEM_INSTRUCTION) }
        )
    }

    /**
     * Asks the advisor a question grounded in [snapshot].
     *
     * Returns live model text when available, otherwise a deterministic summary derived from
     * [snapshot] and prefixed with an explicit offline notice. Never returns fabricated figures.
     */
    suspend fun generateFinancialAdvice(
        prompt: String,
        snapshot: AdvisorSnapshot
    ): String = withContext(Dispatchers.IO) {
        try {
            val response = model.generateContent(
                "User Financial Context:\n${snapshot.toContextBlock()}\n\nUser Question:\n$prompt"
            )
            val text = response.text
            if (text.isNullOrBlank()) offlineSummary(prompt, snapshot, null) else text
        } catch (e: Exception) {
            offlineSummary(prompt, snapshot, e.message)
        }
    }

    /**
     * Deterministic, offline advisor output.
     *
     * Every figure here is read from [snapshot]; nothing is assumed. Replaces the previous
     * hardcoded responses, which asserted specific balances, yields, APRs, named institutions and
     * payoff dates that bore no relation to the user's data.
     */
    private fun offlineSummary(
        prompt: String,
        snapshot: AdvisorSnapshot,
        failureReason: String?
    ): String {
        val header = buildString {
            appendLine("> **Advisor offline.** The AI advisor could not be reached, so the summary")
            appendLine("> below is computed directly from your recorded data — no projections or")
            appendLine("> illustrative figures.")
            if (!failureReason.isNullOrBlank()) {
                // Braces are required: a bare `$failureReason_` makes Kotlin read the trailing
                // markdown underscore as part of the identifier name.
                appendLine("> _Reason: ${failureReason}_")
            }
        }

        if (!snapshot.hasAnyData) {
            return header + """

### No financial data recorded yet
There are no transactions, holdings, debts or goals in this vault, so no metrics can be computed.

### To get a diagnostic
1. Add your income and expenses under Cash Flow.
2. Add holdings under Investments.
3. Add cards and loans under the Debt Center.

Once there is data, this summary reports your actual position.
""".trimIndent()
        }

        val lower = prompt.lowercase(Locale.ROOT)
        val body = when {
            lower.contains("debt") || lower.contains("payoff") ||
                lower.contains("avalanche") || lower.contains("snowball") -> debtSection(snapshot)

            lower.contains("portfolio") || lower.contains("allocation") ||
                lower.contains("sip") || lower.contains("stock") ||
                lower.contains("invest") -> portfolioSection(snapshot)

            lower.contains("cash") || lower.contains("yield") ||
                lower.contains("saving") || lower.contains("uninvested") -> cashSection(snapshot)

            else -> overviewSection(snapshot)
        }

        return header + "\n" + body + "\n\n*Obsidian AI provides data analysis and quantitative " +
            "modelling, not certified personal financial advice.*"
    }

    private fun debtSection(s: AdvisorSnapshot): String {
        if (s.debtAccountCount == 0 || s.totalDebt <= 0.0) {
            return """
### Debt position
No cards or loans are recorded in this vault, so there is no payoff ordering to compute.
""".trimIndent()
        }
        val dti = if (s.monthlyInflow > 0) {
            "${fmtPct(s.dtiPercent)} of recorded monthly inflow"
        } else {
            "not computable — no income recorded"
        }
        return """
### Debt position
- **Total outstanding**: ${s.money(s.totalDebt)} across ${s.debtAccountCount} account(s)
- **Recorded monthly servicing**: ${s.money(s.monthlyDebtServicing)}
- **Debt-to-income**: $dti

### Strategic recommendations
1. Order repayments by interest rate, highest first (avalanche), using the rates on your own
   recorded accounts — this minimises total interest paid.
2. Keep contractual minimums on every other account to avoid penalties.
3. As each balance clears, roll its payment into the next account rather than absorbing it into
   spending.

Exact interest saved and payoff dates require the per-account rate and minimum payment on each
of your accounts; the Debt Center computes these once every account carries its rate.
""".trimIndent()
    }

    private fun portfolioSection(s: AdvisorSnapshot): String {
        if (s.holdingCount == 0 || s.portfolioValue <= 0.0) {
            return """
### Portfolio
No holdings are recorded in this vault, so allocation and return cannot be computed.
""".trimIndent()
        }
        val gain = s.portfolioValue - s.portfolioCost
        val returnLine = if (s.portfolioCost > 0) {
            "${s.money(gain)} (${fmtPct(gain / s.portfolioCost * 100.0)} on cost basis)"
        } else {
            "not computable — no cost basis recorded"
        }
        return """
### Portfolio
- **Current value**: ${s.money(s.portfolioValue)} across ${s.holdingCount} holding(s)
- **Cost basis**: ${s.money(s.portfolioCost)}
- **Unrealised gain/loss**: $returnLine

### Strategic recommendations
1. Review single-position concentration — any holding well above your intended weight raises
   idiosyncratic risk.
2. Keep scheduled contributions running rather than timing entries.
3. Rebalance on a fixed calendar, not on market moves.

Note: the figure above is simple return on cost basis, not a time-weighted or money-weighted
return. A true XIRR needs the date and amount of every contribution.
""".trimIndent()
    }

    private fun cashSection(s: AdvisorSnapshot): String {
        val savings = if (s.monthlyInflow > 0) {
            fmtPct(s.savingsRatePercent)
        } else {
            "not computable — no income recorded"
        }
        return """
### Cash flow
- **Recorded monthly inflow**: ${s.money(s.monthlyInflow)}
- **Recorded monthly outflow**: ${s.money(s.monthlyOutflow)}
- **Net retained**: ${s.money(s.monthlyInflow - s.monthlyOutflow)}
- **Savings rate**: $savings

### Strategic recommendations
1. Size an emergency reserve against your recorded monthly outflow above, then keep it in an
   instrument you can access without penalty.
2. Automate the transfer of retained cash on payday so the decision is not repeated monthly.
3. Review your largest recorded expense categories under Cash Flow for the highest-leverage cut.
""".trimIndent()
    }

    private fun overviewSection(s: AdvisorSnapshot): String {
        val savings = if (s.monthlyInflow > 0) fmtPct(s.savingsRatePercent) else "not computable"
        val dti = if (s.monthlyInflow > 0) fmtPct(s.dtiPercent) else "not computable"
        return """
### Position summary
- **Net worth**: ${s.money(s.netWorth)}
- **Assets**: ${s.money(s.totalAssets)} | **Liabilities**: ${s.money(s.totalLiabilities)}
- **Monthly inflow**: ${s.money(s.monthlyInflow)} | **Outflow**: ${s.money(s.monthlyOutflow)}
- **Savings rate**: $savings | **Debt-to-income**: $dti
- **Recorded**: ${s.transactionCount} transaction(s), ${s.holdingCount} holding(s),
  ${s.debtAccountCount} debt account(s), ${s.goalCount} goal(s)

### Strategic recommendations
1. Keep every account recorded so these metrics stay accurate — gaps understate liabilities.
2. Direct retained cash to the highest-rate debt before new investment while any high-rate
   balance is outstanding.
3. Attach a target date to each goal so required monthly contributions can be computed.
""".trimIndent()
    }

    private fun fmtPct(v: Double): String = String.format(Locale.US, "%.1f%%", v)

    suspend fun parseNaturalLanguageTransaction(input: String): ParsedTransaction? = withContext(Dispatchers.IO) {
        // Quick regex and semantic fallback extractor for instant zero-latency or offline parsing
        val lower = input.lowercase()
        val type = if (lower.contains("salary") || lower.contains("earned") || lower.contains("income") || lower.contains("received") || lower.contains("got paid") || lower.contains("freelance") || lower.contains("dividend")) "INCOME" else "EXPENSE"
        
        // Extract amount: look for KSh, $, or numbers
        val amountRegex = Regex("""(?:(?:ksh|kes|\$|r|₹|€|£)\s*)?(\d+(?:,\d{3})*(?:\.\d{1,2})?)\b""", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(input)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 500.0

        // Extract category
        val category = when {
            lower.contains("grocery") || lower.contains("food") || lower.contains("dinner") || lower.contains("lunch") || lower.contains("coffee") || lower.contains("restaurant") || lower.contains("carrefour") || lower.contains("artcaffe") || lower.contains("java") || lower.contains("supermarket") || lower.contains("naivas") || lower.contains("quickmart") -> "FOOD_DINING"
            lower.contains("uber") || lower.contains("gas") || lower.contains("fuel") || lower.contains("rubis") || lower.contains("total") || lower.contains("shell") || lower.contains("transit") || lower.contains("matatu") || lower.contains("flight") -> "TRANSPORT"
            lower.contains("rent") || lower.contains("mortgage") || lower.contains("home loan") || lower.contains("stanbic") -> "HOUSING"
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("subscription") || lower.contains("cloud") || lower.contains("aws") || lower.contains("chatgpt") -> "SUBSCRIPTIONS"
            lower.contains("kplc") || lower.contains("tokens") || lower.contains("stima") || lower.contains("fiber") || lower.contains("zuku") || lower.contains("water") || lower.contains("utilities") -> "UTILITIES"
            lower.contains("salary") || lower.contains("paycheck") -> "SALARY"
            lower.contains("freelance") || lower.contains("consulting") || lower.contains("retainer") -> "FREELANCE"
            lower.contains("dividend") || lower.contains("coupon") || lower.contains("interest") -> "DIVIDENDS"
            lower.contains("invest") || lower.contains("stock") || lower.contains("sip") || lower.contains("mmf") || lower.contains("safaricom") || lower.contains("bonds") -> "INVESTMENT_SIP"
            lower.contains("clothes") || lower.contains("shoes") || lower.contains("shopping") || lower.contains("jumia") -> "SHOPPING"
            lower.contains("gym") || lower.contains("doctor") || lower.contains("pharmacy") || lower.contains("hospital") || lower.contains("health") -> "HEALTHCARE"
            else -> if (type == "INCOME") "SALARY" else "SHOPPING"
        }

        val account = when {
            lower.contains("mpesa") || lower.contains("m-pesa") -> "M-PESA"
            lower.contains("ncba") -> "NCBA Bank"
            lower.contains("stanbic") -> "Stanbic Bank"
            lower.contains("kcb") -> "KCB Platinum Card"
            lower.contains("sc") || lower.contains("standard chartered") || lower.contains("stanchart") -> "Standard Chartered Card"
            lower.contains("amex") -> "Amex Gold"
            lower.contains("sapphire") || lower.contains("chase") -> "Chase Sapphire"
            else -> "M-PESA"
        }

        ParsedTransaction(
            title = input.take(40).replaceFirstChar { it.uppercase() },
            amount = amount,
            type = type,
            category = category,
            account = account
        )
    }
}

/**
 * The user's actual financial position, assembled by FinanceViewModel from the repository.
 *
 * This is the ONLY channel through which figures reach the advisor. It replaces the previous
 * free-text context string, which mixed real values with hardcoded literals (a fixed "XIRR: 16.8%"
 * and a fixed "Health Score: 91/100") and was only ever inspected for the substring "KES".
 */
data class AdvisorSnapshot(
    val currencySymbol: String,
    val regionTitle: String,
    val fiscalCalendar: String,
    val riskProfile: String,
    val netWorth: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val monthlyInflow: Double,
    val monthlyOutflow: Double,
    val savingsRatePercent: Double,
    val portfolioValue: Double,
    val portfolioCost: Double,
    val totalDebt: Double,
    val monthlyDebtServicing: Double,
    val dtiPercent: Double,
    val transactionCount: Int,
    val holdingCount: Int,
    val debtAccountCount: Int,
    val goalCount: Int
) {
    val hasAnyData: Boolean
        get() = transactionCount > 0 || holdingCount > 0 || debtAccountCount > 0 || goalCount > 0

    fun money(v: Double): String =
        currencySymbol + String.format(Locale.US, "%,.2f", v)

    /**
     * Renders the snapshot for the model, marking empty sections explicitly so the model is told
     * that data is absent rather than being left to fill the gap itself.
     */
    fun toContextBlock(): String = buildString {
        appendLine("Geographical Region: $regionTitle")
        appendLine("Base Currency Symbol: $currencySymbol")
        appendLine("Fiscal Calendar: $fiscalCalendar")
        appendLine("Advisory Risk Profile: $riskProfile")
        appendLine()
        if (!hasAnyData) {
            appendLine("RECORDED DATA: none. This vault is empty — no transactions, holdings,")
            appendLine("debts or goals. Do not state or estimate any figure for this user.")
            return@buildString
        }
        appendLine("Record counts: $transactionCount transactions, $holdingCount holdings, " +
            "$debtAccountCount debt accounts, $goalCount goals.")
        appendLine("Net Worth: ${money(netWorth)}")
        appendLine("Assets: ${money(totalAssets)} | Liabilities: ${money(totalLiabilities)}")
        if (transactionCount > 0) {
            appendLine("Monthly Inflow: ${money(monthlyInflow)} | Outflow: ${money(monthlyOutflow)}")
            appendLine("Savings Rate: ${String.format(Locale.US, "%.1f", savingsRatePercent)}%")
        } else {
            appendLine("Cash flow: no transactions recorded — inflow, outflow and savings rate " +
                "are unavailable.")
        }
        if (holdingCount > 0) {
            appendLine("Portfolio Value: ${money(portfolioValue)} | Cost Basis: ${money(portfolioCost)}")
            appendLine("NOTE: no time-weighted return (XIRR) is available — contribution dates " +
                "are not recorded. Do not state an XIRR figure.")
        } else {
            appendLine("Portfolio: no holdings recorded.")
        }
        if (debtAccountCount > 0) {
            appendLine("Total Debt: ${money(totalDebt)} | Monthly Servicing: ${money(monthlyDebtServicing)}")
            appendLine("Debt-to-Income: ${String.format(Locale.US, "%.1f", dtiPercent)}%")
        } else {
            appendLine("Debt: no cards or loans recorded.")
        }
        appendLine()
        appendLine("Report all amounts in $currencySymbol. State only the figures above.")
    }
}

data class ParsedTransaction(
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val account: String
)

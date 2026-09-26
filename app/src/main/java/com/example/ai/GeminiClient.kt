package com.example.ai

import android.graphics.Bitmap
import com.example.data.util.StatementParser
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject
import java.util.Locale

/**
 * Advisor access to Gemini via Firebase AI Logic.
 *
 * Single configuration constant: [MODEL_NAME] = "gemini-3.6-flash".
 * No raw Gemini API keys in source code.
 */
object GeminiClient {

    private const val MODEL_NAME = "gemini-3.6-flash"

    private const val SYSTEM_INSTRUCTION = """
You are "Obsidian AI", an autonomous wealth management and financial intelligence engine embedded in Obsidian Wealth.

Core Guidelines:
1. Grounding: Use ONLY the figures supplied in the User Financial Context or returned by advisor tools. Never invent, estimate or illustrate a balance, holding, interest rate, institution name, or date.
2. Precision: Compute honestly from user data. Show your arithmetic inputs.
   Currency: The selected currency is a display label only; no FX conversion occurs. Do not claim amounts are converted, and warn before adding values from different source currencies because their numeric sum is not a meaningful converted total.
3. Function Calling: You have access to read-only advisor tools to retrieve detailed spending, debts, goals, recent transactions, and portfolio data. Call tools when details are required.
4. Action Markers:
   - If and only if the user explicitly asks to create or set up a goal, append the exact marker ACTION:OPEN_GOAL_FORM at the very end.
   - If and only if the user explicitly asks to log, add, or record a transaction, append the exact marker ACTION:OPEN_TRANSACTION_FORM at the very end.
   Do not add action markers unless explicitly requested.
5. Tone: Executive, concise, data-driven.
6. Safety: Include this disclosure when discussing adjustments: "Obsidian AI provides data analysis and quantitative modelling, not certified personal financial advice."
"""

    private val getSpendingByCategoryDeclaration = FunctionDeclaration(
        name = "get_spending_by_category",
        description = "Returns category breakdown of expenses over the given number of months.",
        parameters = mapOf("months" to Schema.integer(description = "Number of months to analyze", nullable = false)),
        optionalParameters = listOf("months")
    )

    private val getDebtsDeclaration = FunctionDeclaration(
        name = "get_debts",
        description = "Returns list of recorded credit cards and loans with APR, balance, and servicing costs.",
        parameters = emptyMap(),
        optionalParameters = emptyList()
    )

    private val getGoalsDeclaration = FunctionDeclaration(
        name = "get_goals",
        description = "Returns list of recorded financial goals, progress, target dates, and monthly contributions.",
        parameters = emptyMap(),
        optionalParameters = emptyList()
    )

    private val getRecentTransactionsDeclaration = FunctionDeclaration(
        name = "get_recent_transactions",
        description = "Returns transactions within the last days.",
        parameters = mapOf("days" to Schema.integer(description = "Number of days to inspect", nullable = false)),
        optionalParameters = listOf("days")
    )

    private val getPortfolioDeclaration = FunctionDeclaration(
        name = "get_portfolio",
        description = "Returns holdings and active SIPs/standing orders.",
        parameters = emptyMap(),
        optionalParameters = emptyList()
    )

    private val advisorTools = listOf(
        Tool.functionDeclarations(
            listOf(
                getSpendingByCategoryDeclaration,
                getDebtsDeclaration,
                getGoalsDeclaration,
                getRecentTransactionsDeclaration,
                getPortfolioDeclaration
            )
        )
    )

    private val standardModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.2f
                topP = 0.95f
            },
            tools = advisorTools,
            systemInstruction = content { text(SYSTEM_INSTRUCTION) }
        )
    }

    private val groundedModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.2f
                topP = 0.95f
            },
            tools = advisorTools + listOf(Tool.googleSearch()),
            systemInstruction = content { text(SYSTEM_INSTRUCTION) }
        )
    }

    fun isCurrentRateQuery(prompt: String): Boolean {
        val lower = prompt.lowercase(Locale.ROOT)
        return lower.contains("current t-bill") || lower.contains("treasury rate") ||
                lower.contains("money-market rate") || lower.contains("today's yield") ||
                lower.contains("latest interest rate") || lower.contains("current rate") ||
                lower.contains("treasury yield") || lower.contains("mmf rate")
    }

    /**
     * AI Function calling request with multi-turn tool execution up to 3 rounds.
     */
    suspend fun generateFinancialAdvice(
        prompt: String,
        snapshot: AdvisorSnapshot,
        toolRegistry: AdvisorToolRegistry? = null
    ): String = withContext(Dispatchers.IO) {
        val isGrounded = isCurrentRateQuery(prompt)
        val selectedModel = if (isGrounded) groundedModel else standardModel

        val toolOutputs = StringBuilder()

        try {
            val chat = selectedModel.startChat()
            val userContent = content {
                text("User Financial Context:\n${snapshot.toContextBlock()}\n\nUser Question:\n$prompt")
            }
            var currentResponse = chat.sendMessage(userContent)

            var round = 0
            val maxRounds = 3

            while (currentResponse.functionCalls.isNotEmpty() && round < maxRounds) {
                round++
                val calls = currentResponse.functionCalls
                val responseParts = mutableListOf<FunctionResponsePart>()
                for (call in calls) {
                    val resultJson = toolRegistry?.executeTool(call.name, call.args)
                        ?: buildJsonObject { put("error", "Tool registry unavailable") }
                    toolOutputs.appendLine("Tool [${call.name}]: $resultJson")
                    responseParts.add(FunctionResponsePart(call.name, resultJson))
                }

                currentResponse = chat.sendMessage(
                    Content(
                        role = "user",
                        parts = responseParts
                    )
                )
            }

            var responseText = currentResponse.text ?: ""
            if (responseText.isBlank()) {
                return@withContext offlineSummary(prompt, snapshot, null)
            }

            // Grounding sources extraction
            val candidate = currentResponse.candidates.firstOrNull()
            val chunks = candidate?.groundingMetadata?.groundingChunks ?: emptyList()
            val sources = mutableListOf<String>()
            for (chunk in chunks) {
                val web = chunk.web ?: continue
                val uri = web.uri ?: continue
                val title = web.title?.takeIf { it.isNotBlank() } ?: web.domain ?: uri
                sources.add("[$title]($uri)")
            }
            if (sources.isNotEmpty()) {
                val distinctSources = sources.distinct()
                responseText += "\n\n### Sources\n" + distinctSources.joinToString("\n") { "- $it" }
            }

            // If non-grounded, verify numbers against snapshot + tool outputs
            var finalResult = responseText
            if (!isGrounded) {
                finalResult = performNumericVerification(finalResult, snapshot, toolOutputs.toString())
            }

            finalResult
        } catch (e: Exception) {
            offlineSummary(prompt, snapshot, e.message)
        }
    }

    private fun performNumericVerification(text: String, snapshot: AdvisorSnapshot, toolResults: String): String {
        // Build set of valid numbers
        val validNumbers = mutableSetOf<Double>()
        validNumbers.add(snapshot.netWorth)
        validNumbers.add(snapshot.totalAssets)
        validNumbers.add(snapshot.totalLiabilities)
        validNumbers.add(snapshot.monthlyInflow)
        validNumbers.add(snapshot.monthlyOutflow)
        validNumbers.add(snapshot.savingsRatePercent)
        validNumbers.add(snapshot.portfolioValue)
        validNumbers.add(snapshot.portfolioCost)
        validNumbers.add(snapshot.totalDebt)
        validNumbers.add(snapshot.monthlyDebtServicing)
        validNumbers.add(snapshot.dtiPercent)

        // Parse numbers from tool results
        val numberRegex = Regex("""\b\d+(?:,\d{3})*(?:\.\d+)?\b""")
        numberRegex.findAll(toolResults).forEach { m ->
            m.value.replace(",", "").toDoubleOrNull()?.let { validNumbers.add(it) }
        }

        // Extract numbers from response text
        val matches = numberRegex.findAll(text).map { it.value.replace(",", "").toDoubleOrNull() }.filterNotNull().toList()
        var hasUnmatched = false

        for (num in matches) {
            if (num <= 3.0) continue // Skip small list indexes (1., 2., 3.)
            if (num >= 2020.0 && num <= 2035.0) continue // Skip common years

            val matched = validNumbers.any { valid ->
                Math.abs(valid - num) < 0.1 || (valid > 0 && Math.abs((valid - num) / valid) < 0.02)
            }

            if (!matched) {
                hasUnmatched = true
                break
            }
        }

        return if (hasUnmatched && snapshot.hasAnyData) {
            text + "\n\nVerification warning: one or more figures could not be matched to your recorded data."
        } else {
            text
        }
    }

    /**
     * Receipts photo analysis.
     * Uses Firebase AI Logic to extract merchant, total, date, and category.
     */
    suspend fun analyzeReceiptImage(bitmap: Bitmap): ParsedTransaction = withContext(Dispatchers.IO) {
        try {
            val receiptModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = MODEL_NAME
            )

            val prompt = """
Analyze this receipt image.
Extract:
1. Merchant / Store name
2. Total amount paid as a number. IF THE TOTAL IS UNREADABLE, SMUDGED, OR MISSING, RETURN 0.0. DO NOT GUESS OR INVENT AN AMOUNT.
3. Date if readable in YYYY-MM-DD or readable format (or empty string)
4. Category (one of: FOOD_DINING, TRANSPORT, UTILITIES, SUBSCRIPTIONS, HOUSING, HEALTHCARE, INVESTMENT_SIP, SHOPPING, OTHER)

Return strictly JSON with keys: "merchant", "total", "date", "category".
""".trimIndent()

            val response = receiptModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val rawText = response.text ?: ""
            val jsonStart = rawText.indexOf('{')
            val jsonEnd = rawText.lastIndexOf('}')

            if (jsonStart != -1 && jsonEnd > jsonStart) {
                val jsonStr = rawText.substring(jsonStart, jsonEnd + 1)
                val json = JSONObject(jsonStr)
                val merchant = json.optString("merchant", "Receipt Purchase")
                val total = json.optDouble("total", 0.0)
                val category = json.optString("category", "SHOPPING")
                val rawDate = json.optString("date", "")
                val parsedDate = if (rawDate.isNotBlank()) StatementParser.parseDateString(rawDate) else null
                val needsDateReview = (parsedDate == null)

                ParsedTransaction(
                    title = if (merchant.isBlank()) "Receipt Purchase" else merchant,
                    amount = if (total < 0) 0.0 else total,
                    type = "EXPENSE",
                    category = category,
                    account = "Receipt Log",
                    dateMillis = parsedDate,
                    dateNeedsReview = needsDateReview
                )
            } else {
                ParsedTransaction(
                    title = "Receipt Purchase",
                    amount = 0.0,
                    type = "EXPENSE",
                    category = "SHOPPING",
                    account = "Receipt Log",
                    dateMillis = null,
                    dateNeedsReview = true
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiClient", "Error analyzing receipt image", e)
            ParsedTransaction(
                title = "Receipt Purchase",
                amount = 0.0,
                type = "EXPENSE",
                category = "SHOPPING",
                account = "Receipt Log",
                dateMillis = null,
                dateNeedsReview = true
            )
        }
    }

    /**
     * Deterministic, offline advisor output.
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
        val dti = if (s.monthlyInflow > 0 && s.monthlyDebtServicing > 0.0) {
            "${fmtPct(s.dtiPercent)} of recorded monthly inflow"
        } else {
            "not computable — no recorded loan EMI or income"
        }
        return """
### Debt position
- **Total outstanding**: ${s.money(s.totalDebt)} across ${s.debtAccountCount} account(s)
        - **Recorded loan EMIs per month**: ${s.money(s.monthlyDebtServicing)}
        - **Loan EMI / recent recorded inflow (estimate)**: $dti
        - Card minimums are excluded because issuer terms are unknown; this is not lender-standard DTI.

### Strategic recommendations
1. Order repayments by interest rate, highest first (avalanche), using the rates on your own recorded accounts.
2. Keep contractual minimums on every other account to avoid penalties.
3. As each balance clears, roll its payment into the next account.
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
1. Review single-position concentration.
2. Keep scheduled contributions running.
3. Rebalance on a fixed calendar.
""".trimIndent()
    }

    private fun cashSection(s: AdvisorSnapshot): String {
        val savings = if (s.monthlyInflow > 0) fmtPct(s.savingsRatePercent) else "not computable — no income recorded"
        return """
### Cash flow
- **Recorded monthly inflow**: ${s.money(s.monthlyInflow)}
- **Recorded monthly outflow**: ${s.money(s.monthlyOutflow)}
- **Net retained**: ${s.money(s.monthlyInflow - s.monthlyOutflow)}
- **Savings rate**: $savings

### Strategic recommendations
1. Size an emergency reserve against monthly outflow.
2. Automate payday transfers.
3. Review largest expense categories.
""".trimIndent()
    }

    private fun overviewSection(s: AdvisorSnapshot): String {
        val savings = if (s.monthlyInflow > 0) fmtPct(s.savingsRatePercent) else "not computable"
        val dti = if (s.monthlyInflow > 0 && s.monthlyDebtServicing > 0.0) fmtPct(s.dtiPercent) else "not computable"
        return """
### Position summary
- **Net worth**: ${s.money(s.netWorth)}
- **Assets**: ${s.money(s.totalAssets)} | **Liabilities**: ${s.money(s.totalLiabilities)}
- **Monthly inflow**: ${s.money(s.monthlyInflow)} | **Outflow**: ${s.money(s.monthlyOutflow)}
        - **Savings rate**: $savings | **Recorded loan EMI / recent inflow (estimate)**: $dti
- **Recorded**: ${s.transactionCount} transaction(s), ${s.holdingCount} holding(s), ${s.debtAccountCount} debt account(s), ${s.goalCount} goal(s)

### Strategic recommendations
1. Keep every account recorded so metrics stay accurate.
2. Direct retained cash to highest-rate debt first.
3. Attach target dates to goals.
""".trimIndent()
    }

    private fun fmtPct(v: Double): String = String.format(Locale.US, "%.1f%%", v)

    suspend fun parseNaturalLanguageTransaction(input: String): ParsedTransaction? = withContext(Dispatchers.IO) {
        val lower = input.lowercase(Locale.ROOT)
        val type = if (lower.contains("salary") || lower.contains("earned") || lower.contains("income") || lower.contains("received") || lower.contains("got paid") || lower.contains("freelance") || lower.contains("dividend")) "INCOME" else "EXPENSE"

        val amountRegex = Regex("""(?:(?:ksh|kes|\$|r|₹|€|£)\s*)?(\d+(?:,\d{3})*(?:\.\d{1,2})?)\b""", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(input)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        val category = when {
            lower.contains("grocer") || lower.contains("food") || lower.contains("dinner") || lower.contains("lunch") || lower.contains("coffee") || lower.contains("restaurant") -> "FOOD_DINING"
            lower.contains("uber") || lower.contains("gas") || lower.contains("fuel") || lower.contains("transport") -> "TRANSPORT"
            lower.contains("rent") || lower.contains("mortgage") -> "HOUSING"
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("subscription") -> "SUBSCRIPTIONS"
            lower.contains("kplc") || lower.contains("power") || lower.contains("utilities") -> "UTILITIES"
            lower.contains("salary") || lower.contains("paycheck") -> "SALARY"
            lower.contains("invest") || lower.contains("stock") || lower.contains("sip") -> "INVESTMENT_SIP"
            else -> if (type == "INCOME") "SALARY" else "SHOPPING"
        }

        val account = when {
            lower.contains("mpesa") || lower.contains("m-pesa") -> "M-PESA"
            lower.contains("stanbic") -> "Stanbic Bank"
            lower.contains("chase") -> "Chase Checking"
            lower.contains("cash") -> "Cash Wallet"
            else -> "Checking"
        }

        val parsedDate = StatementParser.parseDateString(input)

        ParsedTransaction(
            title = input.take(40).replaceFirstChar { it.uppercase() },
            amount = amount,
            type = type,
            category = category,
            account = account,
            dateMillis = parsedDate,
            dateNeedsReview = (parsedDate == null)
        )
    }
}

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

    fun toContextBlock(): String = buildString {
        appendLine("Geographical Region: $regionTitle")
        appendLine("Selected display currency label: $currencySymbol (label only; recorded numeric amounts are not converted)")
        appendLine("Fiscal Calendar: $fiscalCalendar")
        appendLine("Advisory Risk Profile: $riskProfile")
        appendLine()
        if (!hasAnyData) {
            appendLine("RECORDED DATA: none. This vault is empty — no transactions, holdings,")
            appendLine("debts or goals. Do not state or estimate any figure for this user.")
            return@buildString
        }
        appendLine("Record counts: $transactionCount transactions, $holdingCount holdings, $debtAccountCount debt accounts, $goalCount goals.")
        appendLine("Net Worth: ${money(netWorth)}")
        appendLine("Assets: ${money(totalAssets)} | Liabilities: ${money(totalLiabilities)}")
        if (transactionCount > 0) {
            appendLine("Monthly Inflow: ${money(monthlyInflow)} | Outflow: ${money(monthlyOutflow)}")
            appendLine("Savings Rate: ${String.format(Locale.US, "%.1f", savingsRatePercent)}%")
        } else {
            appendLine("Cash flow: no transactions recorded.")
        }
        if (holdingCount > 0) {
            appendLine("Portfolio Value: ${money(portfolioValue)} | Cost Basis: ${money(portfolioCost)}")
        } else {
            appendLine("Portfolio: no holdings recorded.")
        }
        if (debtAccountCount > 0) {
            appendLine("Total Debt: ${money(totalDebt)} | Monthly Servicing: ${money(monthlyDebtServicing)}")
            appendLine("Recorded loan EMIs / income recorded in the last 30 days (estimate): ${String.format(Locale.US, "%.1f", dtiPercent)}%.")
            appendLine("Credit-card minimums are excluded because issuer terms are unknown; this is not a lender-standard DTI.")
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
    val account: String,
    val dateMillis: Long? = null,
    val dateNeedsReview: Boolean = false
)

package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ai.GeminiClient
import com.example.data.models.Category
import com.example.data.models.AccountEntity
import com.example.data.models.TransactionEntity
import com.example.data.util.StatementParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.models.AccentColor
import com.example.data.models.CreditCardEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.HoldingType
import com.example.data.models.SipEntity
import com.example.data.models.ThemeMode
import com.example.data.models.TransactionType
import com.example.data.models.SupportedCurrency
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldLight
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ChatMessage

@Composable
fun AddTransactionDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onAdd: (title: String, amount: Double, type: TransactionType, category: Category, account: AccountEntity, note: String) -> Unit,
    onAiSmartLog: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(Category.FOOD_DINING) }
    var account by remember(accounts) { mutableStateOf(accounts.firstOrNull { it.isActive }) }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 560.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log Transaction",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // AI Smart Paste Trigger
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onAiSmartLog() },
                    color = ElectricIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Smart Log: Paste or type natural note",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedType = TransactionType.EXPENSE
                                if (selectedCategory == Category.SALARY || selectedCategory == Category.FREELANCE) {
                                    selectedCategory = Category.FOOD_DINING
                                }
                            },
                        color = if (selectedType == TransactionType.EXPENSE) Color(0x33F43F5E) else ObsidianSurfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (selectedType == TransactionType.EXPENSE) Color(0xFFF43F5E) else ObsidianBorderSubtle
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                "Expense (-)",
                                color = if (selectedType == TransactionType.EXPENSE) Color(0xFFFB7185) else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedType = TransactionType.INCOME
                                selectedCategory = Category.SALARY
                            },
                        color = if (selectedType == TransactionType.INCOME) EmeraldGrowth.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (selectedType == TransactionType.INCOME) EmeraldGrowth else ObsidianBorderSubtle
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                "Income (+)",
                                color = if (selectedType == TransactionType.INCOME) EmeraldLight else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount", color = TextSecondary) },
                    placeholder = { Text("0.00", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description / Merchant", color = TextSecondary) },
                    placeholder = { Text("e.g. Carrefour, Tech Salary, KPLC", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Paid into / from account", color = TextSecondary, fontSize = 11.sp)
                if (accounts.none { it.isActive }) {
                    Text("Create a cash account first. This transaction cannot affect balances until it is linked to an account.", color = TextMuted, fontSize = 11.sp)
                } else {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.filter { it.isActive }.forEach { candidate ->
                            FilterChip(
                                selected = candidate.id == account?.id,
                                onClick = { account = candidate },
                                label = { Text("${candidate.name} Â· ${candidate.currencyCode ?: "?"}", fontSize = 10.sp) }
                            )
                        }
                    }
                    Text("Amount currency: ${account?.currencyCode ?: "unknownâ€”resolve this account first"}", color = if (account?.currencyCode == null) SovereignGold else TextMuted, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amt > 0 && account?.isActive == true && !account?.currencyCode.isNullOrBlank()) {
                            onAdd(title, amt, selectedType, selectedCategory, account!!, note)
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank() && amountText.toDoubleOrNull()?.let { it > 0.0 } == true && account?.isActive == true && !account?.currencyCode.isNullOrBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm & Record", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AiSmartLogDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var isExtracting by remember { mutableStateOf(false) }
    var isReviewMode by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(Category.FOOD_DINING) }
    var account by remember(accounts) { mutableStateOf(accounts.firstOrNull { it.isActive && !it.currencyCode.isNullOrBlank() }) }
    var note by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(0L) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var dateText by remember { mutableStateOf("") }
    var dateNeedsReview by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = CyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isReviewMode) "Confirm Parsed Transaction" else "AI Natural Language Log",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isReviewMode) {
                    Text(
                        text = "Type or speak any natural expense, salary credit, or trade. Obsidian AI extracts fields for your editable review before saving.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick presets
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presets = listOf(
                            "Spent $45 on groceries at Supermarket",
                            "Received $4,500 consulting retainer deposit",
                            "Paid ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬85 for electricity bill",
                            "Spent Ãƒâ€šÃ‚Â£65 on transport and rail ticket",
                            "Invested $500 into Index Fund ETF",
                            "Paid $120 for Internet & Mobile bill"
                        )
                        items(presets) { preset ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { prompt = preset },
                                color = ObsidianSurfaceVariant,
                                border = BorderStroke(1.dp, ObsidianBorderSubtle)
                            ) {
                                Text(
                                    text = preset,
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("e.g. Paid $45 for groceries at Supermarket", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                scope.launch {
                                    isExtracting = true
                                    val parsed = GeminiClient.parseNaturalLanguageTransaction(prompt)
                                    isExtracting = false
                                    if (parsed != null) {
                                        title = parsed.title
                                        amountText = if (parsed.amount > 0.0) parsed.amount.toString() else ""
                                        selectedType = if (parsed.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                                        selectedCategory = try { Category.valueOf(parsed.category) } catch (_: Exception) { Category.OTHER }
                        account = accounts.firstOrNull { it.isActive && it.name.equals(parsed.account, ignoreCase = true) && !it.currencyCode.isNullOrBlank() }
                            ?: accounts.firstOrNull { it.isActive && !it.currencyCode.isNullOrBlank() }
                                        note = "Natural Language Log: \"$prompt\""
                                        val parsedDate = parsed.dateMillis
                                        dateNeedsReview = parsed.dateNeedsReview || parsedDate == null
                                        dateMillis = parsedDate ?: 0L
                                        dateText = parsedDate?.let { dateFormatter.format(Date(it)) } ?: ""
                                        isReviewMode = true
                                    }
                                }
                            }
                        },
                        enabled = prompt.isNotBlank() && !isExtracting,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isExtracting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Extracting DraftÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Extract & Review Draft", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Editable Review Mode
                    if (validationError != null) {
                        Surface(
                            color = Color(0x33EF4444),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = validationError!!,
                                color = Color(0xFFFB7185),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = TransactionType.EXPENSE },
                            label = { Text("Expense", color = if (selectedType == TransactionType.EXPENSE) Color.White else TextSecondary) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE11D48)),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = { selectedType = TransactionType.INCOME },
                            label = { Text("Income", color = if (selectedType == TransactionType.INCOME) Color.Black else TextSecondary) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldGrowth),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            validationError = null
                        },
                        label = { Text("Title / Merchant") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            validationError = null
                        },
                        label = { Text("Amount") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Category", color = TextSecondary, fontSize = 12.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(Category.values().filter { it != Category.ACCOUNT_TRANSFER && it != Category.ACCOUNT_ADJUSTMENT }) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.name.replace("_", " "), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SovereignGold,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Account and currency", color = TextSecondary, fontSize = 11.sp)
                    if (accounts.none { it.isActive && !it.currencyCode.isNullOrBlank() }) {
                        Text("Add a cash account with a confirmed currency before saving.", color = SovereignGold, fontSize = 11.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(accounts.filter { it.isActive && !it.currencyCode.isNullOrBlank() }) { candidate ->
                                FilterChip(
                                    selected = candidate.id == account?.id,
                                    onClick = { account = candidate },
                                    label = { Text("${candidate.name} Â· ${candidate.currencyCode}", fontSize = 10.sp) }
                                )
                            }
                        }
                        Text("Currency: ${account?.currencyCode ?: "choose an account"}", color = TextMuted, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {
                            dateText = it
                            val parsed = StatementParser.parseDateString(it)
                            if (parsed != null) {
                                dateMillis = parsed
                                dateNeedsReview = false
                            } else {
                                dateMillis = 0L
                                dateNeedsReview = true
                            }
                        },
                        label = { Text("Date (YYYY-MM-DD)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (dateNeedsReview || dateMillis <= 0L) {
                        Text(
                            text = "Date could not be read. Enter a valid date before saving.",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { isReviewMode = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                val amt = amountText.toDoubleOrNull()
                                if (title.isBlank()) {
                                    validationError = "Title cannot be blank."
                                } else if (amt == null || amt <= 0.0) {
                                    validationError = "Please enter a valid positive amount."
                                } else if (dateMillis <= 0L || StatementParser.parseDateString(dateText) == null) {
                                    validationError = "Please enter a valid transaction date."
                                } else if (account?.isActive != true || account?.currencyCode.isNullOrBlank()) {
                                    validationError = "Choose an active account with a confirmed currency."
                                } else {
                                    onConfirm(
                                        TransactionEntity(
                                            title = title.trim(),
                                            amount = amt,
                                            type = selectedType,
                                            category = selectedCategory,
                                            account = account!!.name,
                                            dateMillis = dateMillis,
                                            note = note,
                                            accountId = account!!.id,
                                            currencyCode = account!!.currencyCode
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm & Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObsidianAiAdvisorSheet(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenGoalForm: () -> Unit = {},
    onOpenTransactionForm: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(CyanAccent, ElectricIndigo)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Obsidian AI Financial Advisor",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Autonomous Wealth Intelligence Engine",
                            color = CyanAccent,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Alpha Prompt Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                val alphaPrompts = listOf(
                    "Review my recorded cash flow",
                    "Simulate Debt Avalanche vs Snowball",
                    "Explain my recorded holdings",
                    "Check emergency fund runway"
                )
                items(alphaPrompts) { p ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSendMessage(p) },
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = p,
                            color = IndigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chat Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isAi = msg.sender == "AI"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isAi) 2.dp else 16.dp,
                                bottomEnd = if (isAi) 16.dp else 2.dp
                            ),
                            color = if (isAi) ObsidianSurface else ElectricIndigo,
                            border = if (isAi) BorderStroke(1.dp, ObsidianBorder) else null,
                            modifier = Modifier.fillMaxWidth(if (isAi) 0.92f else 0.82f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (isAi) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "AI",
                                            tint = CyanAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Obsidian Intelligence",
                                            color = CyanAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                val rawText = msg.text
                                val hasGoalMarker = rawText.contains("ACTION:OPEN_GOAL_FORM")
                                val hasTxMarker = rawText.contains("ACTION:OPEN_TRANSACTION_FORM")
                                val displayText = rawText
                                    .replace("ACTION:OPEN_GOAL_FORM", "")
                                    .replace("ACTION:OPEN_TRANSACTION_FORM", "")
                                    .trim()

                                Text(
                                    text = displayText,
                                    color = if (isAi) TextPrimary else Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                if (isAi && hasGoalMarker) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onOpenGoalForm()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ÃƒÂ°Ã…Â¸Ã…Â½Ã‚Â¯ Create Goal (Review Form)", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (isAi && hasTxMarker) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onOpenTransactionForm()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ÃƒÂ¢Ã…Â¾Ã¢â‚¬Â¢ Log Transaction (Review Form)", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyanAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Obsidian AI is synthesizing financial models...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask financial questions, simulate models...", color = TextMuted, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(listOf(SovereignGold, GoldLight)),
                            CircleShape
                        )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black)
                }
            }
        }
    }
}

@Composable
fun ExportReportDialog(
    summaryText: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, EmeraldGrowth.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Personal Finance Summary",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = ObsidianBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ObsidianBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        item {
                            Text(
                                text = summaryText,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(summaryText))
                            Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy CSV")
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(summaryText))
                            Toast.makeText(context, "Exported Obsidian P&L Statement", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export P&L", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportedCurrencyChips(selected: SupportedCurrency?, onSelect: (SupportedCurrency) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(SupportedCurrency.values().toList()) { currency ->
            FilterChip(
                selected = currency == selected,
                onClick = { onSelect(currency) },
                label = { Text(currency.code, fontSize = 10.sp) }
            )
        }
    }
}

@Composable
fun AddHoldingDialog(
    defaultCurrency: SupportedCurrency,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onAdd: (symbol: String, name: String, type: HoldingType, shares: Double, avgBuy: Double, current: Double, currencyCode: String, purchaseAccount: AccountEntity?) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(HoldingType.STOCK) }
    var sharesText by remember { mutableStateOf("") }
    var avgBuyText by remember { mutableStateOf("") }
    var currentPriceText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var purchaseAccount by remember { mutableStateOf<AccountEntity?>(null) }
    val matchingAccounts = accounts.filter { it.isActive && it.currencyCode == currency.code }
    // Switching currency can leave a previously chosen account mismatched; clear it rather than submit a mismatch.
    androidx.compose.runtime.LaunchedEffect(currency) {
        if (purchaseAccount != null && purchaseAccount?.currencyCode != currency.code) purchaseAccount = null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Investment Holding",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Ticker / Symbol", color = TextSecondary) },
                    placeholder = { Text("e.g. SCOM, EQTY, VOO, IFB", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset Name", color = TextSecondary) },
                    placeholder = { Text("e.g. MercadoLibre (MELI), Apple (AAPL), Vanguard ETF", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Price currency", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                SupportedCurrencyChips(currency) { currency = it }
                Text("Current and average prices are recorded in ${currency.code}. No FX conversion is applied.", color = TextMuted, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Text("Asset Class Category", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf(
                        HoldingType.STOCK to "Equity Stock",
                        HoldingType.ETF to "Equity ETF",
                        HoldingType.MUTUAL_FUND to "Mutual Fund",
                        HoldingType.GOLD to "Gold / Bullion",
                        HoldingType.CRYPTO to "Crypto"
                    )
                    items(types) { (type, label) ->
                        val isSelected = selectedType == type
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedType = type },
                            color = if (isSelected) EmeraldGrowth.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) EmeraldGrowth else ObsidianBorderSubtle
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) EmeraldLight else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (matchingAccounts.isNotEmpty()) {
                    Text("Pay from account (optional)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pick a bank to deduct this purchase from your cash and list it on Cash Flow for you to confirm. Leave unselected to just track the holding.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matchingAccounts) { candidate ->
                            val isSelected = purchaseAccount?.id == candidate.id
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { purchaseAccount = if (isSelected) null else candidate },
                                color = if (isSelected) EmeraldGrowth.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) EmeraldGrowth else ObsidianBorderSubtle)
                            ) {
                                Text(
                                    text = "${candidate.name} Â· ${candidate.currencyCode}",
                                    color = if (isSelected) EmeraldLight else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(
                        "Add an active account in this currency (Settings) to deduct a purchase from cash and list it on Cash Flow.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sharesText,
                        onValueChange = { sharesText = it },
                        label = { Text("Units / Shares", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldGrowth,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = currentPriceText,
                        onValueChange = { currentPriceText = it },
                        label = { Text("Price", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldGrowth,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val shares = sharesText.toDoubleOrNull() ?: 0.0
                        val current = currentPriceText.toDoubleOrNull() ?: 0.0
                        val avg = avgBuyText.toDoubleOrNull() ?: current
                        if (symbol.isNotBlank() && shares > 0) {
                            onAdd(symbol, name.ifBlank { symbol }, selectedType, shares, avg, current, currency.code, purchaseAccount)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (purchaseAccount != null) "Add & Record Purchase" else "Add to Portfolio",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HoldingActionDialog(
    holding: HoldingEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSell: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(holding.name, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${holding.symbol} ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ ${if (holding.shares % 1.0 == 0.0) holding.shares.toInt() else holding.shares} units", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onEdit(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Holding", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onSell(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sell Shares", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onDelete(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Holding", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun EditHoldingDialog(
    holding: HoldingEntity,
    defaultCurrency: SupportedCurrency,
    onDismiss: () -> Unit,
    onSave: (symbol: String, name: String, type: HoldingType, shares: Double, avgBuy: Double, current: Double, currencyCode: String) -> Unit
) {
    var symbol by remember { mutableStateOf(holding.symbol) }
    var name by remember { mutableStateOf(holding.name) }
    var selectedType by remember { mutableStateOf(holding.type) }
    var sharesText by remember { mutableStateOf(holding.shares.toString()) }
    var avgBuyText by remember { mutableStateOf(holding.avgBuyPrice.toString()) }
    var currentPriceText by remember { mutableStateOf(holding.currentPrice.toString()) }
    var currency by remember { mutableStateOf(SupportedCurrency.values().firstOrNull { it.code == holding.currencyCode } ?: defaultCurrency) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Investment Holding",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Ticker / Symbol", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Price currency", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                SupportedCurrencyChips(currency) { currency = it }
                Text("Choose the currency in which these prices were recorded; no FX conversion is applied.", color = TextMuted, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Text("Asset Class Category", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf(
                        HoldingType.STOCK to "Equity Stock",
                        HoldingType.ETF to "Equity ETF",
                        HoldingType.MUTUAL_FUND to "Mutual Fund",
                        HoldingType.GOLD to "Gold / Bullion",
                        HoldingType.CRYPTO to "Crypto"
                    )
                    items(types) { (type, label) ->
                        val isSelected = selectedType == type
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedType = type },
                            color = if (isSelected) EmeraldGrowth.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) EmeraldGrowth else ObsidianBorderSubtle
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) EmeraldLight else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sharesText,
                        onValueChange = { sharesText = it },
                        label = { Text("Units / Shares", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldGrowth,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = avgBuyText,
                        onValueChange = { avgBuyText = it },
                        label = { Text("Avg Buy Price", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldGrowth,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = currentPriceText,
                    onValueChange = { currentPriceText = it },
                    label = { Text("Current Price", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val shares = sharesText.toDoubleOrNull() ?: holding.shares
                        val current = currentPriceText.toDoubleOrNull() ?: holding.currentPrice
                        val avg = avgBuyText.toDoubleOrNull() ?: holding.avgBuyPrice
                        if (symbol.isNotBlank() && shares > 0) {
                            onSave(symbol.uppercase(), name.ifBlank { symbol }, selectedType, shares, avg, current, currency.code)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteHoldingDialog(
    holdingName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = ObsidianSurface, border = BorderStroke(1.dp, Color(0xFFFF7185)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Delete this holding?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("This permanently removes \"$holdingName\" from your portfolio.", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = { onConfirm(); onDismiss() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))) { Text("Delete", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun AddSipDialog(
    defaultCurrency: SupportedCurrency,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onAdd: (fundName: String, category: String, amount: Double, plannedDay: Int, currencyCode: String, contributionAccount: AccountEntity?) -> Unit
) {
    var fundName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Index Fund") }
    var amountText by remember { mutableStateOf("") }
    var debitDayText by remember { mutableStateOf("1") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var contributionAccount by remember { mutableStateOf<AccountEntity?>(null) }
    val matchingAccounts = accounts.filter { it.isActive && it.currencyCode == currency.code }
    androidx.compose.runtime.LaunchedEffect(currency) {
        if (contributionAccount != null && contributionAccount?.currencyCode != currency.code) contributionAccount = null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add recurring contribution plan",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fundName,
                    onValueChange = { fundName = it },
                    label = { Text("Fund / Mandate Name", color = TextSecondary) },
                    placeholder = { Text("e.g. CIC Money Market Fund, IFB Bond", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Monthly Amount", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldGrowth,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = debitDayText,
                        onValueChange = { debitDayText = it },
                        label = { Text("Planned day of month", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldGrowth,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Contribution currency", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                SupportedCurrencyChips(currency) { currency = it }
                Spacer(modifier = Modifier.height(8.dp))

                if (matchingAccounts.isNotEmpty()) {
                    Text("Fund this month's contribution from (optional)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pick a bank to deduct this month's amount from your cash and list it on Cash Flow for you to confirm. Leave unselected to just save the plan.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matchingAccounts) { candidate ->
                            val isSelected = contributionAccount?.id == candidate.id
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { contributionAccount = if (isSelected) null else candidate },
                                color = if (isSelected) EmeraldGrowth.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) EmeraldGrowth else ObsidianBorderSubtle)
                            ) {
                                Text(
                                    text = "${candidate.name} Â· ${candidate.currencyCode}",
                                    color = if (isSelected) EmeraldLight else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "Add an active account in this currency (Settings) to deduct a contribution from cash and list it on Cash Flow.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        val day = debitDayText.toIntOrNull() ?: 1
                        if (fundName.isNotBlank() && amount > 0) {
                            onAdd(fundName, category, amount, day, currency.code, contributionAccount)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (contributionAccount != null) "Add Plan & Record Contribution" else "Save plan",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SipActionDialog(
    sip: SipEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(sip.fundName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${sip.category} ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ ${sip.monthlyAmount.let { "%,.2f".format(it) }}/mo", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onEdit(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit SIP", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onDelete(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete SIP", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun EditSipDialog(
    sip: SipEntity,
    onDismiss: () -> Unit,
    onSave: (fundName: String, category: String, amount: Double, plannedDay: Int) -> Unit
) {
    var fundName by remember { mutableStateOf(sip.fundName) }
    var category by remember { mutableStateOf(sip.category) }
    var amountText by remember { mutableStateOf(sip.monthlyAmount.toString()) }
    var debitDayText by remember { mutableStateOf(sip.debitDayOfMonth.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit contribution plan",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fundName,
                    onValueChange = { fundName = it },
                    label = { Text("Fund / Mandate Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Monthly Amount", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldGrowth,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = debitDayText,
                        onValueChange = { debitDayText = it },
                        label = { Text("Planned day of month", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldGrowth,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: sip.monthlyAmount
                        val day = debitDayText.toIntOrNull() ?: sip.debitDayOfMonth
                        if (fundName.isNotBlank() && amount > 0) {
                            onSave(fundName, category, amount, day)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteSipDialog(
    fundName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = ObsidianSurface, border = BorderStroke(1.dp, Color(0xFFFF7185)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Delete this SIP?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("This permanently removes the \"$fundName\" SIP mandate. This does not undo any past cash flow entries it already created.", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = { onConfirm(); onDismiss() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))) { Text("Delete", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun PayCreditCardDialog(
    card: CreditCardEntity,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onPay: (Double, AccountEntity) -> Unit
) {
    var amountText by remember { mutableStateOf(card.currentBalance.toString()) }
    var sourceAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull { it.currencyCode == card.currencyCode }) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Record payment to ${card.cardName}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current statement balance: ${"%,.2f".format(card.currentBalance)}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = "This updates your records only; it does not send a payment to your bank or card issuer.",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Text("Paid from account (${card.currencyCode ?: "currency unknown"})", color = TextSecondary, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts.filter { it.isActive && it.currencyCode == card.currencyCode }) { candidate ->
                        FilterChip(
                            selected = candidate.id == sourceAccount?.id,
                            onClick = { sourceAccount = candidate },
                            label = { Text(candidate.name, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { amountText = card.currentBalance.toString() },
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, ObsidianBorderSubtle)
                    ) {
                        Text(
                            text = "Full Balance",
                            color = EmeraldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { amountText = (card.currentBalance * 0.5).toString() },
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, ObsidianBorderSubtle)
                    ) {
                        Text(
                            text = "Half Balance",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            sourceAccount?.let { onPay(amount, it) }
                            onDismiss()
                        }
                    },
                    enabled = sourceAccount != null && sourceAccount?.currencyCode == card.currencyCode && amountText.toDoubleOrNull()?.let { it > 0.0 && it <= card.currentBalance } == true,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Record payment", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
fun AddLoanDialog(
    defaultCurrency: SupportedCurrency,
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Double, Double, Double, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lender by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var remaining by remember { mutableStateOf("") }
    var emi by remember { mutableStateOf("") }
    var apr by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = ObsidianSurface, border = BorderStroke(1.dp, ObsidianBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Loan", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary) }
                }
                Spacer(Modifier.height(12.dp))
                Text("Loan currency", color = TextSecondary, fontSize = 11.sp)
                SupportedCurrencyChips(currency) { currency = it }
                listOf(
                    Triple("Loan name", name) { v: String -> name = v },
                    Triple("Lender", lender) { v: String -> lender = v },
                    Triple("Original amount", total) { v: String -> total = v },
                    Triple("Remaining balance", remaining) { v: String -> remaining = v },
                    Triple("Monthly EMI", emi) { v: String -> emi = v },
                    Triple("Interest rate / APR %", apr) { v: String -> apr = v },
                    Triple("Total term in months", months) { v: String -> months = v }
                ).forEach { (label, value, setter) ->
                    OutlinedTextField(value = value, onValueChange = setter, label = { Text(label, color = TextSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = EmeraldGrowth, unfocusedBorderColor = ObsidianBorder))
                    Spacer(Modifier.height(7.dp))
                }
                Button(onClick = {
                    val values = listOf(total.toDoubleOrNull(), remaining.toDoubleOrNull(), emi.toDoubleOrNull(), apr.toDoubleOrNull())
                    val term = months.toIntOrNull()
                    if (name.isNotBlank() && lender.isNotBlank() && values.all { it != null && it >= 0.0 } && term != null && term > 0) {
                        onAdd(name, lender, values[0]!!, values[1]!!, values[2]!!, values[3]!!, term, currency.code)
                        onDismiss()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Add Loan", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun AddCreditCardDialog(
    defaultCurrency: SupportedCurrency,
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double, Double, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var apr by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("15") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = ObsidianSurface, border = BorderStroke(1.dp, ObsidianBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Credit Card", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary) }
                }
                Spacer(Modifier.height(12.dp))
                Text("Card balance currency", color = TextSecondary, fontSize = 11.sp)
                SupportedCurrencyChips(currency) { currency = it }
                listOf(
                    Triple("Card name", name) { v: String -> name = v },
                    Triple("Current balance", balance) { v: String -> balance = v },
                    Triple("Credit limit", limit) { v: String -> limit = v },
                    Triple("Interest rate / APR %", apr) { v: String -> apr = v },
                    Triple("Due day of month", due) { v: String -> due = v }
                ).forEach { (label, value, setter) ->
                    OutlinedTextField(value = value, onValueChange = setter, label = { Text(label, color = TextSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = EmeraldGrowth, unfocusedBorderColor = ObsidianBorder))
                    Spacer(Modifier.height(7.dp))
                }
                Button(onClick = {
                    val b = balance.toDoubleOrNull(); val l = limit.toDoubleOrNull(); val a = apr.toDoubleOrNull(); val d = due.toIntOrNull()
                    if (name.isNotBlank() && b != null && l != null && a != null && d != null && b >= 0 && l > 0 && d in 1..31) { onAdd(name, b, l, a, d, currency.code); onDismiss() }
                }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Add Credit Card", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    defaultCurrency: SupportedCurrency,
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Double, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Savings") }; var target by remember { mutableStateOf("") }; var current by remember { mutableStateOf("0") }; var monthly by remember { mutableStateOf("") }; var currency by remember { mutableStateOf(defaultCurrency) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = ObsidianSurface, border = BorderStroke(1.dp, ObsidianBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Add Financial Goal", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary) } }
                Spacer(Modifier.height(12.dp))
                listOf(Triple("Goal name", title) { v: String -> title = v }, Triple("Category", category) { v: String -> category = v }, Triple("Target amount", target) { v: String -> target = v }, Triple("Current amount", current) { v: String -> current = v }, Triple("Monthly contribution", monthly) { v: String -> monthly = v }).forEach { (label, value, setter) -> OutlinedTextField(value = value, onValueChange = setter, label = { Text(label, color = TextSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = EmeraldGrowth, unfocusedBorderColor = ObsidianBorder)); Spacer(Modifier.height(7.dp)) }
                Text("Tracking currency", color = TextSecondary, fontSize = 11.sp)
                SupportedCurrencyChips(currency) { currency = it }
                Text("Goal progress is a tracker, not proof of a separate cash asset or money movement.", color = TextMuted, fontSize = 10.sp)
                Button(onClick = { val t = target.toDoubleOrNull(); val c = current.toDoubleOrNull(); val m = monthly.toDoubleOrNull(); if (title.isNotBlank() && t != null && c != null && m != null && t > 0 && c >= 0 && m >= 0) { onAdd(title, category, t, c, m, currency.code); onDismiss() } }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Add Goal", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun ConfirmLoanPaymentDialog(
    accounts: List<AccountEntity>,
    loanName: String,
    paymentAmount: Double,
    currencySymbol: String,
    currencyCode: String?,
    remainingBalance: Double,
    interestRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (AccountEntity) -> Unit
) {
    var sourceAccount by remember(accounts, currencyCode) { mutableStateOf(accounts.firstOrNull { it.isActive && it.currencyCode == currencyCode }) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = ObsidianSurface, border = BorderStroke(1.dp, ElectricIndigo), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                val monthlyInterest = remainingBalance * (interestRate.coerceAtLeast(0.0) / 100.0) / 12.0
                val principalPaid = (paymentAmount - monthlyInterest).coerceIn(0.0, remainingBalance)
                Text("Confirm EMI payment", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Record one manual payment for $loanName?", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Text("Payment: $currencySymbol${String.format("%,.2f", paymentAmount)}", color = EmeraldLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Estimated balance after payment: $currencySymbol${String.format("%,.2f", (remainingBalance - principalPaid).coerceAtLeast(0.0))}", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("This does not run automatically. Tap once for one payment; use it again only for a separate payment period.", color = TextMuted, fontSize = 11.sp)
                Text("Paid from account (${currencyCode ?: "currency unknown"})", color = TextSecondary, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts.filter { it.isActive && it.currencyCode == currencyCode }) { candidate ->
                        FilterChip(selected = candidate.id == sourceAccount?.id, onClick = { sourceAccount = candidate }, label = { Text(candidate.name, fontSize = 10.sp) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = { sourceAccount?.let(onConfirm); onDismiss() }, enabled = sourceAccount != null, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)) { Text("Record payment", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun ConfirmClearDataDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = ObsidianSurface, border = BorderStroke(1.dp, Color(0xFFFF7185)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Clear local data?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("This permanently removes local transactions, investments, SIPs, credit cards, loans, and goals. It does not delete data already stored in the cloud.", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = { onConfirm(); onDismiss() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))) { Text("Clear Data", color = Color.White) }
                }
            }
        }
    }
}


@Composable
fun SellHoldingDialog(
    holding: HoldingEntity,
    accounts: List<AccountEntity>,
    formatAmount: (Double, String?) -> String,
    onDismiss: () -> Unit,
    onSell: (shares: Double, price: Double, proceedsAccount: AccountEntity) -> Unit
) {
    var proceedsAccount by remember(accounts, holding.currencyCode) { mutableStateOf(accounts.firstOrNull { it.isActive && it.currencyCode == holding.currencyCode }) }
    var sharesText by remember { mutableStateOf(holding.shares.toString()) }
    var priceText by remember { mutableStateOf(holding.currentPrice.toString()) }
    val shares = sharesText.toDoubleOrNull()
    val price = priceText.toDoubleOrNull()
    val valid = shares != null && price != null && shares > 0.0 && shares <= holding.shares + 1e-9 && price >= 0.0 && proceedsAccount != null
    val proceeds = if (valid) shares!! * price!! else 0.0
    val gain = if (valid) shares!! * (price!! - holding.avgBuyPrice) else 0.0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Sell ${holding.symbol}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("You hold ${holding.shares} units. Record sale proceeds as a same-currency transfer into a cash account, not income.", color = TextSecondary, fontSize = 12.sp)
                Text("Sale price and proceeds currency: ${holding.currencyCode ?: "unknown"}", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = sharesText,
                    onValueChange = { sharesText = it },
                    label = { Text("Units to sell", color = TextSecondary) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Sale price per unit", color = TextSecondary) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Proceeds account", color = TextSecondary, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts.filter { it.isActive && it.currencyCode == holding.currencyCode }) { candidate ->
                        FilterChip(selected = candidate.id == proceedsAccount?.id, onClick = { proceedsAccount = candidate }, label = { Text(candidate.name, fontSize = 10.sp) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (valid) {
                    Text("Proceeds: ${formatAmount(proceeds, holding.currencyCode)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${if (gain >= 0) "Recorded gain estimate" else "Recorded loss estimate"}: ${formatAmount(Math.abs(gain), holding.currencyCode)}",
                        color = if (gain >= 0) EmeraldLight else Color(0xFFFB7185),
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { proceedsAccount?.let { onSell(shares!!, price!!, it) }; onDismiss() },
                    enabled = valid,
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm sale", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun ThemePickerDialog(
    currentMode: ThemeMode,
    currentAccent: AccentColor,
    onDismiss: () -> Unit,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Appearance", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Theme", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.values().forEach { mode ->
                        val selected = mode == currentMode
                        Surface(
                            onClick = { onModeChange(mode) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else ObsidianBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = mode.title,
                                color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Accent color", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccentColor.values().forEach { accent ->
                        val selected = accent == currentAccent
                        val swatch = Color(accent.argb)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .then(
                                    if (selected) Modifier.border(2.dp, TextPrimary, CircleShape)
                                    else Modifier.border(1.dp, ObsidianBorder, CircleShape)
                                )
                                .clickable { onAccentChange(accent) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "${accent.title} selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(currentAccent.title, color = TextMuted, fontSize = 11.sp)

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

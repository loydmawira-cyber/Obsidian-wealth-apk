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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.example.data.models.TransactionEntity
import com.example.data.util.StatementParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.models.CreditCardEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.HoldingType
import com.example.data.models.SipEntity
import com.example.data.models.TransactionType
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
    onDismiss: () -> Unit,
    onAdd: (title: String, amount: Double, type: TransactionType, category: Category, account: String, note: String) -> Unit,
    onAiSmartLog: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(Category.FOOD_DINING) }
    var account by remember { mutableStateOf("M-Pesa / Bank") }
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

                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("Account / Source", color = TextSecondary) },
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
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amt > 0) {
                            onAdd(title, amt, selectedType, selectedCategory, account, note)
                            onDismiss()
                        }
                    },
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
    var account by remember { mutableStateOf("M-PESA") }
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
                            "Paid â‚¬85 for electricity bill",
                            "Spent Â£65 on transport and rail ticket",
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
                                        account = parsed.account
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
                                Text("Extracting Draftâ€¦", color = Color.Black, fontWeight = FontWeight.Bold)
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
                        items(Category.values()) { cat ->
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

                    OutlinedTextField(
                        value = account,
                        onValueChange = { account = it },
                        label = { Text("Account") },
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
                                } else {
                                    onConfirm(
                                        TransactionEntity(
                                            title = title.trim(),
                                            amount = amt,
                                            type = selectedType,
                                            category = selectedCategory,
                                            account = account.trim().ifEmpty { "Default" },
                                            dateMillis = dateMillis,
                                            note = note
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
                    "Audit my cash drag yield",
                    "Simulate Debt Avalanche vs Snowball",
                    "Review portfolio beta & XIRR",
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
                                        Text("ðŸŽ¯ Create Goal (Review Form)", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                        Text("âž• Log Transaction (Review Form)", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        text = "Audited Financial Statement",
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
fun AddHoldingDialog(
    onDismiss: () -> Unit,
    onAdd: (symbol: String, name: String, type: HoldingType, shares: Double, avgBuy: Double, current: Double) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(HoldingType.STOCK) }
    var sharesText by remember { mutableStateOf("") }
    var avgBuyText by remember { mutableStateOf("") }
    var currentPriceText by remember { mutableStateOf("") }

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
                            onAdd(symbol, name.ifBlank { symbol }, selectedType, shares, avg, current)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to Portfolio", color = Color.Black, fontWeight = FontWeight.Bold)
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
                Text("${holding.symbol} â€¢ ${if (holding.shares % 1.0 == 0.0) holding.shares.toInt() else holding.shares} units", color = TextSecondary, fontSize = 13.sp)
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
    onDismiss: () -> Unit,
    onSave: (symbol: String, name: String, type: HoldingType, shares: Double, avgBuy: Double, current: Double) -> Unit
) {
    var symbol by remember { mutableStateOf(holding.symbol) }
    var name by remember { mutableStateOf(holding.name) }
    var selectedType by remember { mutableStateOf(holding.type) }
    var sharesText by remember { mutableStateOf(holding.shares.toString()) }
    var avgBuyText by remember { mutableStateOf(holding.avgBuyPrice.toString()) }
    var currentPriceText by remember { mutableStateOf(holding.currentPrice.toString()) }

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
                            onSave(symbol.uppercase(), name.ifBlank { symbol }, selectedType, shares, avg, current)
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
    onDismiss: () -> Unit,
    onAdd: (fundName: String, category: String, amount: Double, debitDay: Int, annualizedReturnPercent: Double) -> Unit
) {
    var fundName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Index Fund") }
    var amountText by remember { mutableStateOf("") }
    var debitDayText by remember { mutableStateOf("1") }
    var returnPercentText by remember { mutableStateOf("") }

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
                        text = "Schedule New SIP Mandate",
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
                        label = { Text("Debit Day", color = TextSecondary) },
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
                    value = returnPercentText,
                    onValueChange = { returnPercentText = it },
                    label = { Text("Expected Annual Return %", color = TextSecondary) },
                    placeholder = { Text("e.g. 12.5", color = TextMuted) },
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
                        val day = debitDayText.toIntOrNull() ?: 1
                        val returnPercent = returnPercentText.toDoubleOrNull() ?: 0.0
                        if (fundName.isNotBlank() && amount > 0) {
                            onAdd(fundName, category, amount, day, returnPercent)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Activate SIP Mandate", color = Color.Black, fontWeight = FontWeight.Bold)
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
                Text("${sip.category} â€¢ ${sip.monthlyAmount.let { "%,.2f".format(it) }}/mo", color = TextSecondary, fontSize = 13.sp)
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
    onSave: (fundName: String, category: String, amount: Double, debitDay: Int, annualizedReturnPercent: Double) -> Unit
) {
    var fundName by remember { mutableStateOf(sip.fundName) }
    var category by remember { mutableStateOf(sip.category) }
    var amountText by remember { mutableStateOf(sip.monthlyAmount.toString()) }
    var debitDayText by remember { mutableStateOf(sip.debitDayOfMonth.toString()) }
    var returnPercentText by remember { mutableStateOf(sip.annualizedReturnPercent.toString()) }

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
                        text = "Edit SIP Mandate",
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
                        label = { Text("Debit Day", color = TextSecondary) },
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
                    value = returnPercentText,
                    onValueChange = { returnPercentText = it },
                    label = { Text("Expected Annual Return %", color = TextSecondary) },
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
                        val amount = amountText.toDoubleOrNull() ?: sip.monthlyAmount
                        val day = debitDayText.toIntOrNull() ?: sip.debitDayOfMonth
                        val returnPercent = returnPercentText.toDoubleOrNull() ?: sip.annualizedReturnPercent
                        if (fundName.isNotBlank() && amount > 0) {
                            onSave(fundName, category, amount, day, returnPercent)
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
    onDismiss: () -> Unit,
    onPay: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(card.currentBalance.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Pay ${card.cardName}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current statement balance: ${"%,.2f".format(card.currentBalance)}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

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
                            .clickable { amountText = (card.currentBalance * 0.1).coerceAtLeast(35.0).toString() },
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, ObsidianBorderSubtle)
                    ) {
                        Text(
                            text = "Minimum Due",
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
                            onPay(amount)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Authorize Payment", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
fun AddLoanDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Double, Double, Double, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lender by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var remaining by remember { mutableStateOf("") }
    var emi by remember { mutableStateOf("") }
    var apr by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = ObsidianSurface, border = BorderStroke(1.dp, ObsidianBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Loan", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary) }
                }
                Spacer(Modifier.height(12.dp))
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
                        onAdd(name, lender, values[0]!!, values[1]!!, values[2]!!, values[3]!!, term)
                        onDismiss()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Add Loan", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun AddCreditCardDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double, Double, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var apr by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("15") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = ObsidianSurface, border = BorderStroke(1.dp, ObsidianBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Credit Card", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary) }
                }
                Spacer(Modifier.height(12.dp))
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
                    if (name.isNotBlank() && b != null && l != null && a != null && d != null && b >= 0 && l > 0 && d in 1..31) { onAdd(name, b, l, a, d); onDismiss() }
                }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Add Credit Card", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Double, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Savings") }; var target by remember { mutableStateOf("") }; var current by remember { mutableStateOf("0") }; var monthly by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = ObsidianSurface, border = BorderStroke(1.dp, ObsidianBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Add Financial Goal", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold); IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary) } }
                Spacer(Modifier.height(12.dp))
                listOf(Triple("Goal name", title) { v: String -> title = v }, Triple("Category", category) { v: String -> category = v }, Triple("Target amount", target) { v: String -> target = v }, Triple("Current amount", current) { v: String -> current = v }, Triple("Monthly contribution", monthly) { v: String -> monthly = v }).forEach { (label, value, setter) -> OutlinedTextField(value = value, onValueChange = setter, label = { Text(label, color = TextSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = EmeraldGrowth, unfocusedBorderColor = ObsidianBorder)); Spacer(Modifier.height(7.dp)) }
                Button(onClick = { val t = target.toDoubleOrNull(); val c = current.toDoubleOrNull(); val m = monthly.toDoubleOrNull(); if (title.isNotBlank() && t != null && c != null && m != null && t > 0 && c >= 0 && m >= 0) { onAdd(title, category, t, c, m); onDismiss() } }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Add Goal", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun ConfirmLoanPaymentDialog(
    loanName: String,
    paymentAmount: Double,
    currencySymbol: String,
    remainingBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = ObsidianSurface, border = BorderStroke(1.dp, ElectricIndigo), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Confirm EMI payment", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Record one manual payment for $loanName?", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Text("Payment: $currencySymbol${String.format("%,.2f", paymentAmount)}", color = EmeraldLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Balance after payment: $currencySymbol${String.format("%,.2f", (remainingBalance - paymentAmount).coerceAtLeast(0.0))}", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("This does not run automatically. Tap once for one payment; use it again only for a separate payment period.", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = { onConfirm(); onDismiss() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)) { Text("Confirm EMI", color = Color.White) }
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
    formatAmount: (Double) -> String,
    onDismiss: () -> Unit,
    onSell: (shares: Double, price: Double) -> Unit
) {
    var sharesText by remember { mutableStateOf(holding.shares.toString()) }
    var priceText by remember { mutableStateOf(holding.currentPrice.toString()) }
    val shares = sharesText.toDoubleOrNull()
    val price = priceText.toDoubleOrNull()
    val valid = shares != null && price != null && shares > 0.0 && shares <= holding.shares + 1e-9 && price >= 0.0
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
                Text("You hold ${holding.shares} units. Proceeds are added to Cash Flow as income.", color = TextSecondary, fontSize = 12.sp)
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
                Spacer(Modifier.height(10.dp))
                if (valid) {
                    Text("Proceeds: ${formatAmount(proceeds)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${if (gain >= 0) "Realised gain" else "Realised loss"}: ${formatAmount(Math.abs(gain))}",
                        color = if (gain >= 0) EmeraldLight else Color(0xFFFB7185),
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onSell(shares!!, price!!); onDismiss() },
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

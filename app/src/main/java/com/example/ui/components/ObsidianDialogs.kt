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
import com.example.data.models.Category
import com.example.data.models.CreditCardEntity
import com.example.data.models.HoldingType
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
    onSubmit: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = CyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Natural Language Log",
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
                Text(
                    text = "Type or speak any natural expense, salary credit, or trade. Obsidian AI extracts amount, category, and account.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = listOf(
                        "Paid KSh 4,500 for groceries at Carrefour via M-Pesa",
                        "Received KSh 450,000 consulting retainer in Stanbic",
                        "Paid KSh 12,500 for Kenya Power tokens",
                        "Invested KSh 50,000 into CIC Money Market Fund",
                        "Paid KSh 3,200 at Artcaffe with NCBA card",
                        "Spent $48 on Uber airport ride"
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
                    placeholder = { Text("e.g. Paid KSh 3,200 at Java House on M-Pesa", color = TextMuted) },
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
                            onSubmit(prompt)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto-Parse & Insert", color = Color.Black, fontWeight = FontWeight.Bold)
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
    onDismiss: () -> Unit
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
                                Text(
                                    text = msg.text,
                                    color = if (isAi) TextPrimary else Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
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
                    placeholder = { Text("e.g. Safaricom PLC, Equity Group, IFB Bond", color = TextMuted) },
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
fun AddSipDialog(
    onDismiss: () -> Unit,
    onAdd: (fundName: String, category: String, amount: Double, debitDay: Int) -> Unit
) {
    var fundName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Index Fund") }
    var amountText by remember { mutableStateOf("") }
    var debitDayText by remember { mutableStateOf("1") }

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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        val day = debitDayText.toIntOrNull() ?: 1
                        if (fundName.isNotBlank() && amount > 0) {
                            onAdd(fundName, category, amount, day)
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

package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ParsedTransaction
import com.example.data.models.Category
import com.example.data.models.AccountEntity
import com.example.data.models.TransactionType
import com.example.data.util.StatementParser
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatementImportDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    var rawText by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var parsedList by remember { mutableStateOf<List<com.example.data.util.ParsedTransaction>?>(null) }
    val scope = rememberCoroutineScope()

    if (parsedList != null) {
        StatementPreviewDialog(
            parsedTransactions = parsedList!!,
            accounts = accounts,
            onConfirm = { selected, account ->
                viewModel.addImportedTransactions(selected, account)
                onDismiss()
            },
            onDismiss = { parsedList = null }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Statement Import Engine", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Paste your CSV lines, M-Pesa transaction SMS texts, or bank statement records below. The parser uses deterministic duplicate prevention.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("Paste CSV or SMS statements here...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SovereignGold,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedContainerColor = ObsidianSurfaceVariant,
                        unfocusedContainerColor = ObsidianSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isParsing = true
                        val parsed = viewModel.parseStatement(rawText)
                        parsedList = parsed
                        isParsing = false
                    }
                },
                enabled = rawText.isNotBlank() && !isParsing,
                colors = ButtonDefaults.buttonColors(containerColor = SovereignGold)
            ) {
                if (isParsing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Parsing…", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Parse Statement", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun StatementPreviewDialog(
    parsedTransactions: List<com.example.data.util.ParsedTransaction>,
    accounts: List<AccountEntity>,
    onConfirm: (List<com.example.data.util.ParsedTransaction>, AccountEntity) -> Unit,
    onDismiss: () -> Unit
) {
    // Only pre-select transactions that are not duplicates and not ambiguous
    val selectedIndices = remember {
        mutableStateListOf<Int>().apply {
            parsedTransactions.indices.forEach { idx ->
                val tx = parsedTransactions[idx]
                if (!tx.isDuplicate && !tx.dateAmbiguous && tx.dateMillis > 0L) {
                    add(idx)
                }
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var selectedAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull { it.isActive && !it.currencyCode.isNullOrBlank() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Review Statement Records (${parsedTransactions.size})", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (parsedTransactions.isEmpty()) {
                Text("No valid transaction records detected in the provided text.", color = TextMuted, fontSize = 13.sp)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column {
                            Text("Import account and currency", color = TextSecondary, fontSize = 11.sp)
                            if (accounts.none { it.isActive && !it.currencyCode.isNullOrBlank() }) {
                                Text("Create an account with a confirmed currency before importing.", color = Color(0xFFF59E0B), fontSize = 11.sp)
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(accounts.filter { it.isActive && !it.currencyCode.isNullOrBlank() }) { candidate ->
                                    FilterChip(
                                        selected = candidate.id == selectedAccount?.id,
                                        onClick = { selectedAccount = candidate },
                                        label = { Text("${candidate.name} · ${candidate.currencyCode}", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                    items(parsedTransactions.size) { idx ->
                        val tx = parsedTransactions[idx]
                        val isSelected = selectedIndices.contains(idx)
                        val isBlocked = tx.isDuplicate || tx.dateAmbiguous || tx.dateMillis <= 0L

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (tx.isDuplicate) ObsidianSurfaceVariant.copy(alpha = 0.5f) else ObsidianSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                when {
                                    tx.isDuplicate -> ObsidianBorderSubtle
                                    tx.dateAmbiguous -> Color(0xFFF59E0B)
                                    else -> ObsidianBorder
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked && !isBlocked) selectedIndices.add(idx) else selectedIndices.remove(idx)
                                        },
                                        enabled = !isBlocked,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = SovereignGold,
                                            disabledCheckedColor = TextMuted,
                                            disabledUncheckedColor = TextMuted
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.title, color = if (isBlocked) TextSecondary else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        val dateStr = if (tx.dateMillis > 0L) dateFormatter.format(Date(tx.dateMillis)) else "No Date"
                                        Text("${tx.category.name} • ${tx.account} • $dateStr", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Text(
                                        text = (if (tx.type == TransactionType.INCOME) "+" else "-") + String.format(Locale.US, "%.2f", tx.amount),
                                        color = if (isBlocked) TextMuted else if (tx.type == TransactionType.INCOME) EmeraldLight else Color(0xFFFB7185),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (tx.isDuplicate) {
                                    Text(
                                        text = "Duplicate record: already imported into this vault",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                                    )
                                } else if (tx.dateAmbiguous || tx.dateMillis <= 0L) {
                                    Text(
                                        text = "Ambiguous date: excluded to protect timeline accuracy",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = selectedIndices.map { parsedTransactions[it] }
                    selectedAccount?.let { onConfirm(selected, it) }
                },
                enabled = selectedIndices.isNotEmpty() && selectedAccount != null,
                colors = ButtonDefaults.buttonColors(containerColor = SovereignGold)
            ) {
                Text("Confirm Import (${selectedIndices.size})", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back", color = TextSecondary)
            }
        }
    )
}

@Composable
fun ReceiptPhotoDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    var isAnalyzing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<ParsedTransaction?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isAnalyzing = true
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                viewModel.analyzeReceipt(bitmap) { result ->
                    draft = result
                    isAnalyzing = false
                }
            } catch (e: Exception) {
                isAnalyzing = false
            }
        }
    }

    if (draft != null) {
        ReceiptReviewDialog(
            draft = draft!!,
            accounts = accounts,
            onConfirm = { merchant, amount, categoryStr, account, dateMillis ->
                val cat = try { Category.valueOf(categoryStr) } catch (_: Exception) { Category.SHOPPING }
                viewModel.addTransaction(
                    title = merchant,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = cat,
                    account = account,
                    dateMillis = dateMillis,
                    note = "Receipt Photo Import"
                )
                onDismiss()
            },
            onDismiss = onDismiss
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Receipt Photo Logging", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (isAnalyzing) {
                    CircularProgressIndicator(color = CyanAccent, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Analyzing receipt image with Firebase AI...", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Text(
                        "Select a photo of your receipt. Firebase AI will extract merchant, amount, date, and category for review. Original photo is not saved by default.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            if (!isAnalyzing) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Choose Receipt Photo", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isAnalyzing) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    )
}

@Composable
fun ReceiptReviewDialog(
    draft: ParsedTransaction,
    accounts: List<AccountEntity>,
    onConfirm: (merchant: String, amount: Double, category: String, account: AccountEntity, dateMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var merchant by remember { mutableStateOf(draft.title) }
    var amountText by remember { mutableStateOf(if (draft.amount > 0) draft.amount.toString() else "") }
    var account by remember(accounts) { mutableStateOf(accounts.firstOrNull { it.isActive && it.name.equals(draft.account, true) && !it.currencyCode.isNullOrBlank() } ?: accounts.firstOrNull { it.isActive && !it.currencyCode.isNullOrBlank() }) }
    var selectedCategory by remember { mutableStateOf(draft.category) }
    val initialDateMillis = draft.dateMillis ?: System.currentTimeMillis()
    var dateMillis by remember { mutableStateOf(initialDateMillis) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var dateText by remember { mutableStateOf(dateFormatter.format(Date(initialDateMillis))) }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val isAmountUncertain = draft.amount <= 0.0 || amountValue <= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Review Receipt Draft", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isAmountUncertain) {
                    Surface(
                        color = Color(0x33F59E0B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Text(
                            "Total amount could not be unambiguously read from the image. Please verify or enter the total below.",
                            color = SovereignGold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (draft.dateNeedsReview) {
                    Surface(
                        color = Color(0x2238BDF8),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyanAccent)
                    ) {
                        Text(
                            "Date was not clearly visible on receipt; defaulted to today. Adjust below if needed.",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Store") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SovereignGold,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Total Amount") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = if (isAmountUncertain) SovereignGold else EmeraldLight,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        val parsed = StatementParser.parseDateString(it)
                        if (parsed != null) {
                            dateMillis = parsed
                        }
                    },
                    label = { Text("Date (YYYY-MM-DD)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SovereignGold,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Cash account and currency", color = TextSecondary, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts.filter { it.isActive && !it.currencyCode.isNullOrBlank() }) { candidate ->
                        FilterChip(selected = candidate.id == account?.id, onClick = { account = candidate }, label = { Text("${candidate.name} · ${candidate.currencyCode}", fontSize = 10.sp) })
                    }
                }
                if (accounts.none { it.isActive && !it.currencyCode.isNullOrBlank() }) {
                    Text("Create a cash account with a confirmed currency before saving this receipt.", color = Color(0xFFF59E0B), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    account?.let { onConfirm(merchant, amountValue, selectedCategory, it, dateMillis) }
                },
                enabled = merchant.isNotBlank() && amountValue.isFinite() && amountValue > 0 && account != null,
                colors = ButtonDefaults.buttonColors(containerColor = SovereignGold)
            ) {
                Text("Confirm & Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

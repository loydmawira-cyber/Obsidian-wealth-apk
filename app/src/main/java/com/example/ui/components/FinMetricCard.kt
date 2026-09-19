package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardGradientEnd
import com.example.ui.theme.CardGradientStart
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldGradientStart
import com.example.ui.theme.GoldLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FinCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    border: BorderStroke? = BorderStroke(1.dp, ObsidianBorder),
    backgroundColor: Color = ObsidianSurface,
    gradient: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (gradient != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(16.dp),
                content = content
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun HeroGradientCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    FinCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        border = BorderStroke(1.dp, GoldBorder),
        gradient = Brush.linearGradient(
            colors = listOf(
                GoldGradientStart,
                CardGradientStart,
                ObsidianSurface
            )
        ),
        onClick = onClick,
        content = content
    )
}

@Composable
fun GoldPrestigeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    FinCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        border = BorderStroke(1.5.dp, SovereignGold),
        gradient = Brush.linearGradient(
            colors = listOf(
                GoldGradientStart,
                CardGradientStart,
                ObsidianSurface
            )
        ),
        onClick = onClick,
        content = content
    )
}

@Composable
fun MetricBadge(
    text: String,
    isPositive: Boolean = true,
    modifier: Modifier = Modifier,
    backgroundColor: Color = if (isPositive) SovereignGold.copy(alpha = 0.18f) else Color(0x22F43F5E),
    textColor: Color = if (isPositive) GoldLight else Color(0xFFFB7185)
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        border = BorderStroke(0.5.dp, if (isPositive) SovereignGold.copy(alpha = 0.6f) else textColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = (if (isPositive && !text.startsWith("+") && !text.startsWith("-")) "+" else "") + text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GoldBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = SovereignGold.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = GoldLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ActionPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ObsidianSurfaceVariant,
    iconTint: Color = SovereignGold
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconTint.copy(alpha = 0.15f), CircleShape)
                    .border(0.5.dp, iconTint.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

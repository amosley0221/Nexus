package com.nexus.launcher.ui.hub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.launcher.domain.BudgetCategory
import com.nexus.launcher.domain.BudgetState
import com.nexus.launcher.domain.Transaction
import com.nexus.launcher.ui.common.Badge
import com.nexus.launcher.ui.common.NexusCard
import com.nexus.launcher.ui.common.NexusProgressBar
import com.nexus.launcher.ui.common.Squircle
import com.nexus.launcher.ui.theme.LocalWindowProfile
import com.nexus.launcher.ui.theme.NexusColor
import com.nexus.launcher.ui.theme.NexusRadius
import com.nexus.launcher.ui.theme.NexusType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BudgetPage(
    budget: BudgetState,
    currency: String = "€",
    modifier: Modifier = Modifier,
    onAddTransaction: () -> Unit,
    onPeriodClick: () -> Unit,
) {
    val profile = LocalWindowProfile.current
    val monthLabel = remember {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    }

    HubScaffold(
        title = "Budget",
        modifier = modifier,
        trailing = {
            Text(
                text = "$monthLabel ▾",
                style = NexusType.BodySmall,
                color = NexusColor.TextSecondary,
                modifier = Modifier.clickable(onClick = onPeriodClick),
            )
        },
    ) {
        RingCard(budget = budget, currency = currency)

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            budget.categories.take(profile.hubGridColumns + 1).forEach { category ->
                CategoryCard(
                    category = category,
                    currency = currency,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionRow(
            title = "Recent transactions",
            linkText = "View all →",
            onLinkClick = onAddTransaction,
        )
        Spacer(Modifier.height(8.dp))

        // Scrolls internally so the ring and category cards stay pinned.
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(budget.transactions, key = { it.id }) { transaction ->
                TransactionRow(transaction = transaction, currency = currency)
            }
        }
    }
}

@Composable
private fun RingCard(budget: BudgetState, currency: String) {
    NexusCard(modifier = Modifier.fillMaxWidth(), radius = NexusRadius.CardLarge) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(110.dp)) {
                    val stroke = 13.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = NexusColor.Border,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = NexusColor.Accent,
                        startAngle = -90f,
                        sweepAngle = 360f * budget.fractionUsed,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatMoney(budget.spentCents, currency),
                        style = NexusType.PageTitle.copy(fontSize = 20.sp),
                        color = NexusColor.TextPrimary,
                    )
                    Text(
                        text = "${(budget.fractionUsed * 100).toInt()}% used",
                        style = NexusType.Caption,
                        color = NexusColor.TextSecondary,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "REMAINING",
                    style = NexusType.Caption.copy(fontWeight = FontWeight.Bold),
                    color = NexusColor.TextSecondary,
                )
                Text(
                    text = formatMoney(budget.remainingCents, currency),
                    style = NexusType.PageTitle.copy(fontSize = 26.sp),
                    color = NexusColor.TextPrimary,
                )
                Text(
                    text = "of ${formatMoney(budget.totalLimitCents, currency)} · " +
                        "${daysLeftInMonth()} days left",
                    style = NexusType.Meta,
                    color = NexusColor.TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                val onTrack = budget.fractionUsed <= elapsedFractionOfMonth()
                Badge(
                    text = if (onTrack) "↗ On track" else "↘ Over pace",
                    background = if (onTrack) NexusColor.GreenBg else NexusColor.AmberBg,
                    foreground = if (onTrack) NexusColor.Green else NexusColor.Amber,
                    radius = 10.dp,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: BudgetCategory,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val fraction = if (category.limitCents <= 0) {
        0f
    } else {
        (category.spentCents.toFloat() / category.limitCents)
    }
    val overBudget = fraction > 1f

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(NexusRadius.Card))
            .background(NexusColor.Card)
            .border(1.dp, NexusColor.Border, RoundedCornerShape(NexusRadius.Card))
            .padding(12.dp),
    ) {
        Text(
            text = category.name,
            style = NexusType.Meta,
            color = NexusColor.TextSecondary,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatMoney(category.spentCents, currency),
            style = NexusType.SectionHeader.copy(fontSize = 17.sp),
            color = NexusColor.TextPrimary,
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        NexusProgressBar(
            progress = fraction.coerceIn(0f, 1f),
            color = if (overBudget) NexusColor.Amber else NexusColor.Accent,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, currency: String) {
    val timeLabel = remember(transaction.epochMillis) { relativeTime(transaction.epochMillis) }
    val positive = transaction.amountCents >= 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
    ) {
        Squircle(size = 36.dp, background = categoryColor(transaction.category)) {
            Text(
                text = categoryGlyph(transaction.category),
                style = NexusType.CardTitle,
                color = NexusColor.TextPrimary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.name,
                style = NexusType.CardTitleSemi,
                color = NexusColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = timeLabel, style = NexusType.Meta, color = NexusColor.TextSecondary)
        }
        Text(
            text = (if (positive) "+" else "−") +
                formatMoney(kotlin.math.abs(transaction.amountCents), currency),
            style = NexusType.CardTitleSemi,
            color = if (positive) NexusColor.Green else NexusColor.Negative,
        )
    }
}

private fun categoryColor(category: String) = when (category.lowercase()) {
    "food" -> NexusColor.XlsBg
    "transport" -> NexusColor.DocBg
    "fun" -> NexusColor.PptBg
    "income" -> NexusColor.GreenBg
    else -> NexusColor.Card
}

private fun categoryGlyph(category: String) = when (category.lowercase()) {
    "food" -> "▤"
    "transport" -> "➤"
    "fun" -> "♪"
    "income" -> "↓"
    else -> "•"
}

private fun formatMoney(cents: Long, currency: String): String {
    val whole = cents / 100
    val fraction = (cents % 100).toInt()
    return if (fraction == 0) {
        "$currency${"%,d".format(whole)}"
    } else {
        "$currency${"%,d".format(whole)}.${fraction.toString().padStart(2, '0')}"
    }
}

private fun daysLeftInMonth(): Int {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_MONTH)
    val total = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    return (total - today).coerceAtLeast(0)
}

private fun elapsedFractionOfMonth(): Float {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_MONTH).toFloat()
    val total = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).toFloat()
    return (today / total).coerceIn(0f, 1f)
}

private fun relativeTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val calendarNow = Calendar.getInstance().apply { timeInMillis = now }
    val calendarThen = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    val sameDay = calendarNow.get(Calendar.YEAR) == calendarThen.get(Calendar.YEAR) &&
        calendarNow.get(Calendar.DAY_OF_YEAR) == calendarThen.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return "Today, ${timeFormat.format(Date(epochMillis))}"

    calendarNow.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = calendarNow.get(Calendar.YEAR) == calendarThen.get(Calendar.YEAR) &&
        calendarNow.get(Calendar.DAY_OF_YEAR) == calendarThen.get(Calendar.DAY_OF_YEAR)
    if (yesterday) return "Yesterday"

    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMillis))
}

package com.ailauncher.app.ui.screens

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.ailauncher.app.data.UsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v6 feature #6: Usage statistics screen with top apps, screen time, launch count.
 *
 * v10: reads UsageStatsManager directly (UsageStatsRepository.getRangeTotals).
 * It used to read a Room table that nothing ever populated, so it was always
 * empty. Rows show the app's name and icon instead of its package name, and the
 * page explains how to grant usage access when it's missing.
 */
@Composable
fun StatisticsScreen(usageStatsRepo: UsageStatsRepository, onResetLearning: () -> Unit) {
    var topApps by remember { mutableStateOf<List<UsageStatsRepository.AppUsageTotal>>(emptyList()) }
    var selectedRange by remember { mutableStateOf(StatsRange.WEEK) }
    var hasPermission by remember { mutableStateOf(true) }
    var resetDone by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(selectedRange) {
        hasPermission = withContext(Dispatchers.Default) { usageStatsRepo.hasPermission() }
        val cal = Calendar.getInstance()
        when (selectedRange) {
            StatsRange.DAY -> cal.add(Calendar.DAY_OF_YEAR, -1)
            StatsRange.WEEK -> cal.add(Calendar.DAY_OF_YEAR, -7)
            StatsRange.MONTH -> cal.add(Calendar.MONTH, -1)
        }
        topApps = usageStatsRepo.getRangeTotals(cal.timeInMillis, excludePackage = context.packageName).take(20)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Spacer(Modifier.height(16.dp))

        if (!hasPermission) {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.stats_permission_needed), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    TextButton(onClick = {
                        try { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (_: Exception) {}
                    }) { Text(stringResource(R.string.stats_grant)) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Range selector
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsRange.entries.forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { selectedRange = range },
                    label = { Text(stringResource(range.displayNameRes)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Summary card
        val totalScreenTimeMs = topApps.sumOf { it.screenTimeMs }
        val totalLaunches = topApps.sumOf { it.launches }
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                SummaryItem(stringResource(R.string.stats_screen_time), formatDuration(context, totalScreenTimeMs), Icons.Rounded.AccessTime)
                SummaryItem(stringResource(R.string.stats_launches), "$totalLaunches", Icons.Rounded.OpenInNew)
                SummaryItem(stringResource(R.string.stats_apps), "${topApps.size}", Icons.Rounded.Apps)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.stats_top_apps),
            modifier = Modifier.padding(horizontal = 16.dp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(topApps, key = { it.packageName }) { app ->
                AppStatRow(
                    packageName = app.packageName,
                    screenTimeMs = app.screenTimeMs,
                    launches = app.launches,
                    maxScreenTime = topApps.firstOrNull()?.screenTimeMs ?: 1L
                )
            }
            item {
                // v10: forget what "Recommended now" learned from taps/ignores.
                TextButton(onClick = { onResetLearning(); resetDone = true }, enabled = !resetDone) {
                    Text(stringResource(if (resetDone) R.string.stats_reset_learning_done else R.string.stats_reset_learning))
                }
            }
        }
    }
}

enum class StatsRange(@StringRes val displayNameRes: Int) {
    DAY(R.string.stats_range_day),
    WEEK(R.string.stats_range_week),
    MONTH(R.string.stats_range_month)
}

@Composable
private fun RowScope.SummaryItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppStatRow(packageName: String, screenTimeMs: Long, launches: Int, maxScreenTime: Long) {
    val context = LocalContext.current
    val ratio = if (maxScreenTime > 0) (screenTimeMs.toFloat() / maxScreenTime) else 0f
    // v10: show what the user recognises — the app's name and icon, not its package.
    val (label, icon) = remember(packageName) {
        val pm = context.packageManager
        try {
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString() to runCatching { pm.getApplicationIcon(ai).toBitmap(72, 72).asImageBitmap() }.getOrNull()
        } catch (_: Exception) { packageName to null }
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    androidx.compose.foundation.Image(icon, null, Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)))
                    Spacer(Modifier.width(8.dp))
                }
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(formatDuration(context, screenTimeMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            // Bar chart
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background)) {
                Box(Modifier.fillMaxWidth(ratio).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.stats_launches_count, launches), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDuration(context: Context, ms: Long): String {
    val hours = (ms / 3_600_000).toInt()
    val minutes = ((ms % 3_600_000) / 60_000).toInt()
    return when {
        hours > 0 -> context.getString(R.string.duration_hours_minutes, hours, minutes)
        else -> context.getString(R.string.duration_minutes, minutes)
    }
}

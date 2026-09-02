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
import com.ailauncher.app.data.db.DailyStatsEntity
import com.ailauncher.app.data.db.UsageDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * v6 feature #6: Usage statistics screen with top apps, screen time, launch count.
 */
@Composable
fun StatisticsScreen(usageDao: UsageDao) {
    var topApps by remember { mutableStateOf<List<DailyStatsEntity>>(emptyList()) }
    var selectedRange by remember { mutableStateOf(StatsRange.WEEK) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(selectedRange) {
        val cal = Calendar.getInstance()
        when (selectedRange) {
            StatsRange.DAY -> cal.add(Calendar.DAY_OF_YEAR, -1)
            StatsRange.WEEK -> cal.add(Calendar.DAY_OF_YEAR, -7)
            StatsRange.MONTH -> cal.add(Calendar.MONTH, -1)
        }
        val fromDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        topApps = usageDao.getTopAppsByScreenTime(fromDate, 20)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Spacer(Modifier.height(16.dp))

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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(topApps) { app ->
                AppStatRow(
                    packageName = app.packageName,
                    screenTimeMs = app.screenTimeMs,
                    launches = app.launches,
                    maxScreenTime = topApps.firstOrNull()?.screenTimeMs ?: 1L
                )
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
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(packageName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
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

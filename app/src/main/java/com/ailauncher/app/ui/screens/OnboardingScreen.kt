package com.ailauncher.app.ui.screens

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.ailauncher.app.R
import kotlinx.coroutines.launch

data class OnboardingStep(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val actionRes: Int? = null,
    val actionType: String? = null
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val steps = listOf(
        OnboardingStep(Icons.Rounded.AutoAwesome, R.string.onboarding_welcome_title, R.string.onboarding_welcome_desc),
        OnboardingStep(Icons.Rounded.QueryStats, R.string.onboarding_usage_title, R.string.onboarding_usage_desc,
            R.string.onboarding_usage_action, "usage_stats"),
        OnboardingStep(Icons.Rounded.SwipeRight, R.string.onboarding_gestures_title, R.string.onboarding_gestures_desc),
        OnboardingStep(Icons.Rounded.Palette, R.string.onboarding_personalize_title, R.string.onboarding_personalize_desc),
        OnboardingStep(Icons.Rounded.Home, R.string.onboarding_default_title, R.string.onboarding_default_desc,
            R.string.onboarding_default_action, "set_default"),
    )

    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().padding(24.dp)
    ) {
        // Skip button
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onComplete) { Text(stringResource(R.string.action_skip), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val step = steps[page]
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(step.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(32.dp))
                Text(stringResource(step.titleRes), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(step.descriptionRes), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 24.sp)

                if (step.actionRes != null) {
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        when (step.actionType) {
                            "usage_stats" -> context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            "set_default" -> {
                                // v8 FIX: previously both branches were identical (Settings.ACTION_HOME_SETTINGS).
                                // On API 29+ RoleManager is the documented way to request the HOME role;
                                // it shows the actual "set as default home app" dialog instead of dumping
                                // the user into a settings list.
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(RoleManager::class.java)
                                    if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true &&
                                        !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                                        try {
                                            context.startActivity(
                                                roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                                            )
                                        } catch (_: Exception) {
                                            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                        }
                                    } else {
                                        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                    }
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                }
                            }
                        }
                    }, shape = RoundedCornerShape(16.dp)) {
                        Text(stringResource(step.actionRes))
                    }
                }
            }
        }

        // Page indicators
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(steps.size) { i ->
                Box(Modifier.padding(4.dp).size(if (pagerState.currentPage == i) 10.dp else 6.dp)
                    .clip(CircleShape).background(if (pagerState.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Next / Finish
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) { Text(stringResource(R.string.action_previous)) }
            } else { Spacer(Modifier.width(1.dp)) }

            Button(onClick = {
                if (pagerState.currentPage == steps.lastIndex) onComplete()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }, shape = RoundedCornerShape(16.dp)) {
                Text(stringResource(if (pagerState.currentPage == steps.lastIndex) R.string.onboarding_start else R.string.action_next))
            }
        }
    }
}

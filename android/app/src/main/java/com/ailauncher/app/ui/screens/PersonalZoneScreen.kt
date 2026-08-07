package com.ailauncher.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.LockMethod
import com.ailauncher.app.ui.LauncherViewModel
import kotlinx.coroutines.launch

@Composable
fun PersonalZoneScreen(viewModel: LauncherViewModel) {
    val security by viewModel.security.collectAsState()
    val hidden by viewModel.hiddenApps.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isUnlocked by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val privateApps = remember(hidden) {
        val pm = context.packageManager
        hidden.privateFolderPackages.mapNotNull { pkg ->
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(info).toString()
                val icon = pm.getApplicationIcon(pkg)
                Triple(pkg, label, icon)
            } catch (_: Exception) { null }
        }.sortedBy { it.second }
    }

    val wrongCodeText = stringResource(R.string.auth_wrong)

    if (!isUnlocked && security.personalZonePin.isNotEmpty()) {
        // Lock screen
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.personal_zone_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(stringResource(R.string.personal_zone_enter_pin), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp); Spacer(Modifier.height(8.dp)) }
            OutlinedTextField(
                value = pinInput, onValueChange = { pinInput = it },
                label = { Text(stringResource(R.string.security_method_pin)) }, visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.width(200.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                scope.launch {
                    if (viewModel.appLockManager.verifyPersonalZonePin(pinInput) ||
                        viewModel.appLockManager.verifyPin(pinInput)) {
                        isUnlocked = true; error = null
                    } else { error = wrongCodeText; pinInput = "" }
                }
            }, shape = RoundedCornerShape(16.dp)) { Text(stringResource(R.string.action_enter)) }
        }
    } else {
        // Unlocked — show private apps
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.personal_zone_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.weight(1f))
                if (security.personalZonePin.isNotEmpty()) {
                    IconButton(onClick = { isUnlocked = false; pinInput = "" }) {
                        Icon(Icons.Rounded.Lock, stringResource(R.string.action_lock), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (privateApps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.FolderOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.personal_zone_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.personal_zone_add_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(privateApps) { (pkg, label, icon) ->
                        val bitmap = com.ailauncher.app.ui.LocalIconCache.current.getOrLoad(pkg) { icon }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { launchApp(context, pkg) }.padding(4.dp)) {
                            if (bitmap != null) Image(bitmap = bitmap, contentDescription = label, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)))
                            Spacer(Modifier.height(4.dp))
                            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.width(72.dp))
                        }
                    }
                }
            }
        }
    }
}

package com.ailauncher.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ailauncher.app.R
import com.ailauncher.app.domain.models.AppCategory
import com.ailauncher.app.domain.models.AppProfile
import com.ailauncher.app.domain.models.RankedApp
import com.ailauncher.app.domain.models.SmartFolder
import com.ailauncher.app.ui.LauncherViewModel
import com.ailauncher.app.ui.launchApp
import com.ailauncher.app.ui.theme.launcherFontFamily

@Composable
fun AppsScreen(
    viewModel: LauncherViewModel,
    gridColumns: Int,
) {
    val appState by viewModel.appState.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val query by viewModel.debouncedSearchQuery.collectAsState()
    val context = LocalContext.current
    // v8: long-press target shared between grid + folders.
    var longPressTarget by remember { mutableStateOf<RankedApp?>(null) }
    // v10: Personal / Work filter, shown only when a work profile exists.
    var profileFilter by rememberSaveable { mutableStateOf(ProfileFilter.ALL) }

    // v9: honour the SmartControl haptic setting on long-press (see HomeScreen).
    val smartControl by viewModel.smartControl.collectAsState()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(longPressTarget) {
        if (longPressTarget != null && smartControl.hapticFeedback) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val workProfile = appState.profiles.firstOrNull { it.profile == AppProfile.WORK }
    val privateSpace = appState.profiles.firstOrNull { it.profile == AppProfile.PRIVATE }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        SearchBar(
            query = appState.searchQuery,
            onQueryChange = { viewModel.updateSearch(it) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // View mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(if (viewMode == LauncherViewModel.ViewMode.GRID) R.string.all_apps else R.string.smart_folders),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { viewModel.toggleViewMode() }) {
                Text(
                    text =
                        stringResource(
                            if (viewMode ==
                                LauncherViewModel.ViewMode.GRID
                            ) {
                                R.string.view_mode_folders
                            } else {
                                R.string.view_mode_list
                            },
                        ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (workProfile != null && viewMode == LauncherViewModel.ViewMode.GRID) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileFilter.entries.forEach { f ->
                    FilterChip(selected = profileFilter == f, onClick = { profileFilter = f }, label = { Text(stringResource(f.labelRes)) })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (appState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            when (viewMode) {
                LauncherViewModel.ViewMode.GRID -> {
                    val apps =
                        remember(appState.rankedApps, query, profileFilter) {
                            viewModel.filteredApps().filter {
                                when (profileFilter) {
                                    ProfileFilter.ALL -> it.app.profile != AppProfile.PRIVATE
                                    ProfileFilter.PERSONAL -> it.app.profile == AppProfile.PERSONAL || it.app.profile == AppProfile.CLONE
                                    ProfileFilter.WORK -> it.app.profile == AppProfile.WORK
                                }
                            }
                        }
                    AppGrid(
                        apps = apps,
                        gridColumns = gridColumns,
                        iconSizeDp = appearance.iconSizeDp,
                        fontSizeSp = appearance.fontSizeSp,
                        iconShape = appearance.iconShape,
                        font = appearance.appFont,
                        onAppClick = { launchApp(context, it.app) },
                        onAppLongClick = { longPressTarget = it },
                        header =
                            if (workProfile != null && workProfile.isQuietModeEnabled && profileFilter != ProfileFilter.PERSONAL) {
                                {
                                    ProfileBanner(
                                        text = stringResource(R.string.work_apps_paused),
                                        action = stringResource(R.string.work_apps_turn_on),
                                        onAction = { viewModel.setProfileQuietMode(workProfile.userSerial, false) },
                                    )
                                }
                            } else {
                                null
                            },
                        footer =
                            if ((privateSpace != null && profileFilter == ProfileFilter.ALL && query.isBlank()) ||
                                (workProfile != null && !workProfile.isQuietModeEnabled && profileFilter == ProfileFilter.WORK)
                            ) {
                                {
                                    if (privateSpace != null && profileFilter == ProfileFilter.ALL && query.isBlank()) {
                                        PrivateSpaceSection(
                                            locked = privateSpace.isQuietModeEnabled,
                                            apps = appState.privateSpaceApps,
                                            gridColumns = gridColumns,
                                            iconSizeDp = appearance.iconSizeDp,
                                            fontSizeSp = appearance.fontSizeSp,
                                            iconShape = appearance.iconShape,
                                            font = appearance.appFont,
                                            onToggle = {
                                                viewModel.setProfileQuietMode(
                                                    privateSpace.userSerial,
                                                    !privateSpace.isQuietModeEnabled,
                                                )
                                            },
                                            onAppClick = { launchApp(context, it.app) },
                                            onAppLongClick = { longPressTarget = it },
                                        )
                                    }
                                    if (workProfile != null && !workProfile.isQuietModeEnabled && profileFilter == ProfileFilter.WORK) {
                                        TextButton(onClick = { viewModel.setProfileQuietMode(workProfile.userSerial, true) }) {
                                            Text(stringResource(R.string.work_apps_pause))
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                    )
                }

                LauncherViewModel.ViewMode.CATEGORIES -> {
                    FolderView(
                        folders = appState.smartFolders,
                        expandedFolder = appState.expandedFolder,
                        folderIconSizeDp = appearance.folderIconSizeDp,
                        folderFontSizeSp = appearance.folderFontSizeSp,
                        folderFont = appearance.folderFont,
                        onFolderClick = { viewModel.expandFolder(it) },
                        onAppClick = { launchApp(context, it.app) },
                    )
                }
            }
        }
    }

    longPressTarget?.let { target ->
        AppActionsSheet(target = target, viewModel = viewModel, onDismiss = { longPressTarget = null })
    }
}

private enum class ProfileFilter(
    val labelRes: Int,
) {
    ALL(R.string.profile_filter_all),
    PERSONAL(R.string.profile_filter_personal),
    WORK(R.string.profile_filter_work),
}

@Composable
private fun ProfileBanner(
    text: String,
    action: String,
    onAction: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), fontSize = 14.sp)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/**
 * v10: Android 15 Private Space. Locked → a single row with an unlock button
 * (the OS asks for the space's own credential). Unlocked → its apps, plus a
 * lock button. These apps never appear in suggestions, folders or search
 * while the space is locked — see GetRankedAppsUseCase.
 */
@Composable
private fun PrivateSpaceSection(
    locked: Boolean,
    apps: List<RankedApp>,
    gridColumns: Int,
    iconSizeDp: Int,
    fontSizeSp: Int,
    iconShape: com.ailauncher.app.domain.models.IconShape,
    font: com.ailauncher.app.domain.models.LauncherFont,
    onToggle: () -> Unit,
    onAppClick: (RankedApp) -> Unit,
    onAppLongClick: (RankedApp) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.private_space_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onToggle) {
                    Text(stringResource(if (locked) R.string.private_space_unlock else R.string.private_space_lock))
                }
            }
            if (!locked && apps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                apps.chunked(gridColumns).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { app ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                HomeAppItem(
                                    app,
                                    iconSizeDp,
                                    fontSizeSp,
                                    iconShape,
                                    font,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(app) },
                                )
                            }
                        }
                        repeat(gridColumns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        if (query.isEmpty()) {
            Text(stringResource(R.string.search_apps_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AppGrid(
    apps: List<RankedApp>,
    gridColumns: Int,
    iconSizeDp: Int,
    fontSizeSp: Int,
    iconShape: com.ailauncher.app.domain.models.IconShape,
    font: com.ailauncher.app.domain.models.LauncherFont,
    onAppClick: (RankedApp) -> Unit,
    onAppLongClick: ((RankedApp) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().testTag("apps_grid"),
    ) {
        // Only real content gets a row — an empty one still costs the grid's spacing.
        header?.let { h -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") { h() } }
        // v10: AppInfo.key, not packageName — one package can have several
        // launcher activities and can exist in more than one profile, and
        // duplicate keys crash a Lazy grid.
        items(apps, key = { it.app.key }) { rankedApp ->
            HomeAppItem(
                rankedApp = rankedApp,
                iconSizeDp = iconSizeDp,
                fontSizeSp = fontSizeSp,
                iconShape = iconShape,
                font = font,
                onClick = { onAppClick(rankedApp) },
                onLongClick = onAppLongClick?.let { cb -> { cb(rankedApp) } },
            )
        }
        footer?.let { f -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") { f() } }
    }
}

@Composable
fun FolderView(
    folders: List<SmartFolder>,
    expandedFolder: AppCategory?,
    folderIconSizeDp: Int,
    folderFontSizeSp: Int,
    folderFont: com.ailauncher.app.domain.models.LauncherFont,
    onFolderClick: (AppCategory?) -> Unit,
    onAppClick: (RankedApp) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(folders, key = { it.category.name }) { folder ->
            FolderCard(
                folder = folder,
                isExpanded = expandedFolder == folder.category,
                iconSizeDp = folderIconSizeDp,
                fontSizeSp = folderFontSizeSp,
                font = folderFont,
                onClick = {
                    onFolderClick(if (expandedFolder == folder.category) null else folder.category)
                },
                onAppClick = onAppClick,
            )
        }
    }
}

@Composable
fun FolderCard(
    folder: SmartFolder,
    isExpanded: Boolean,
    iconSizeDp: Int,
    fontSizeSp: Int,
    font: com.ailauncher.app.domain.models.LauncherFont,
    onClick: () -> Unit,
    onAppClick: (RankedApp) -> Unit,
) {
    // Settings → Fonts → Folders: the folder name, its count and the app list inside.
    val fontFamily = launcherFontFamily(font)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(folder.category.displayNameRes),
                    fontSize = fontSizeSp.sp,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = fontFamily,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${folder.apps.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = fontFamily,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                folder.apps.take(4).forEach { app ->
                    val icon = app.app.icon
                    val bitmap =
                        icon?.let { drw ->
                            com.ailauncher.app.ui.LocalIconCache.current
                                .getOrLoad(app.app.key) { drw }
                        }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.app.label,
                            modifier = Modifier.size(iconSizeDp.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
            val reduceMotion = com.ailauncher.app.ui.LocalReduceMotion.current
            AnimatedVisibility(
                visible = isExpanded,
                enter = if (reduceMotion) EnterTransition.None else fadeIn() + expandVertically(),
                exit = if (reduceMotion) ExitTransition.None else fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    folder.apps.forEach { rankedApp ->
                        androidx.compose.runtime.key(rankedApp.app.key) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onAppClick(rankedApp) }
                                        .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val icon = rankedApp.app.icon
                                val bitmap =
                                    icon?.let { drw ->
                                        com.ailauncher.app.ui.LocalIconCache.current
                                            .getOrLoad(rankedApp.app.key) { drw }
                                    }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    rankedApp.app.label,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = fontFamily,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

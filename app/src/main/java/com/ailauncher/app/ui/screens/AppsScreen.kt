package com.ailauncher.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.ailauncher.app.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ailauncher.app.domain.models.AppCategory
import com.ailauncher.app.domain.models.RankedApp
import com.ailauncher.app.domain.models.SmartFolder
import com.ailauncher.app.ui.LauncherViewModel

@Composable
fun AppsScreen(viewModel: LauncherViewModel, gridColumns: Int) {
    val appState by viewModel.appState.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val context = LocalContext.current
    // v8: long-press target shared between grid + folders.
    var longPressTarget by remember { mutableStateOf<RankedApp?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        SearchBar(
            query = appState.searchQuery,
            onQueryChange = { viewModel.updateSearch(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // View mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(if (viewMode == LauncherViewModel.ViewMode.GRID) R.string.all_apps else R.string.smart_folders),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { viewModel.toggleViewMode() }) {
                Text(
                    text = stringResource(if (viewMode == LauncherViewModel.ViewMode.GRID) R.string.view_mode_folders else R.string.view_mode_list),
                    color = MaterialTheme.colorScheme.primary
                )
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
                    AppGrid(
                        apps = viewModel.filteredApps(),
                        gridColumns = gridColumns,
                        iconSizeDp = appearance.iconSizeDp,
                        fontSizeSp = appearance.fontSizeSp,
                        iconShape = appearance.iconShape,
                        font = appearance.appFont,
                        onAppClick = { launchApp(context, it.app.packageName) },
                        onAppLongClick = { longPressTarget = it }
                    )
                }
                LauncherViewModel.ViewMode.CATEGORIES -> {
                    FolderView(
                        folders = appState.smartFolders,
                        expandedFolder = appState.expandedFolder,
                        folderIconSizeDp = appearance.folderIconSizeDp,
                        folderFontSizeSp = appearance.folderFontSizeSp,
                        onFolderClick = { viewModel.expandFolder(it) },
                        onAppClick = { launchApp(context, it.app.packageName) }
                    )
                }
            }
        }
    }

    longPressTarget?.let { target ->
        AppActionsSheet(
            target = target,
            onDismiss = { longPressTarget = null },
            onHide = { viewModel.hideApp(target.app.packageName); longPressTarget = null },
            onPrivate = { viewModel.moveToPrivateFolder(target.app.packageName); longPressTarget = null },
            onLockToggle = { viewModel.toggleAppLock(target.app.packageName); longPressTarget = null },
            onAppInfo = {
                try {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(android.net.Uri.parse("package:${target.app.packageName}"))
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {}
                longPressTarget = null
            },
            onUninstall = {
                try {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_DELETE)
                            .setData(android.net.Uri.parse("package:${target.app.packageName}"))
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {}
                longPressTarget = null
            }
        )
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (query.isEmpty()) {
            Text(stringResource(R.string.search_apps_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
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
    onAppLongClick: ((RankedApp) -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(apps, key = { it.app.packageName }) { rankedApp ->
            HomeAppItem(
                rankedApp = rankedApp,
                iconSizeDp = iconSizeDp,
                fontSizeSp = fontSizeSp,
                iconShape = iconShape,
                font = font,
                onClick = { onAppClick(rankedApp) },
                onLongClick = onAppLongClick?.let { cb -> { cb(rankedApp) } }
            )
        }
    }
}

@Composable
fun FolderView(
    folders: List<SmartFolder>,
    expandedFolder: AppCategory?,
    folderIconSizeDp: Int,
    folderFontSizeSp: Int,
    onFolderClick: (AppCategory?) -> Unit,
    onAppClick: (RankedApp) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders, key = { it.category.name }) { folder ->
            FolderCard(
                folder = folder,
                isExpanded = expandedFolder == folder.category,
                iconSizeDp = folderIconSizeDp,
                fontSizeSp = folderFontSizeSp,
                onClick = {
                    onFolderClick(if (expandedFolder == folder.category) null else folder.category)
                },
                onAppClick = onAppClick
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
    onClick: () -> Unit,
    onAppClick: (RankedApp) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(folder.category.displayNameRes),
                    fontSize = fontSizeSp.sp,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text("${folder.apps.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                folder.apps.take(4).forEach { app ->
                    val icon = app.app.icon
                    val bitmap = icon?.let { drw ->
                        com.ailauncher.app.ui.LocalIconCache.current.getOrLoad(app.app.packageName) { drw }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.size(iconSizeDp.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    folder.apps.forEach { rankedApp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClick(rankedApp) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = rankedApp.app.icon
                            val bitmap = icon?.let { drw ->
                                com.ailauncher.app.ui.LocalIconCache.current.getOrLoad(rankedApp.app.packageName) { drw }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(rankedApp.app.label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

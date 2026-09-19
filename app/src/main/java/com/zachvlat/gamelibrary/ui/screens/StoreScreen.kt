package com.zachvlat.gamelibrary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zachvlat.gamelibrary.library.GameLibrary
import com.zachvlat.gamelibrary.library.model.GameInfo
import com.zachvlat.gamelibrary.library.model.Store
import com.zachvlat.gamelibrary.library.store.steam.SteamKeyTestResult
import com.zachvlat.gamelibrary.library.ui.ItchAutoSyncWebViewDialog
import com.zachvlat.gamelibrary.library.ui.LoginWebViewDialog
import kotlinx.coroutines.launch

private enum class SortOption {
    ALPHA_ASC,
    ALPHA_DESC,
    RELEASE_DESC,
    RELEASE_ASC
}

@Composable
fun StoreScreen(
    store: Store,
    library: GameLibrary,
    games: List<GameInfo>,
    onGamesUpdated: (List<GameInfo>) -> Unit,
    onGameClick: (GameInfo) -> Unit = {},
    onAddGame: () -> Unit = {},
    onEditGame: (GameInfo) -> Unit = {},
    onDeleteGame: ((GameInfo) -> Unit)? = null
) {
    var isLoggedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var loginUrl by remember { mutableStateOf("") }
    var showLoginDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.ALPHA_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showItchAutoSyncDialog by remember { mutableStateOf(false) }
    var itchAutoSyncUrl by remember { mutableStateOf("") }
    var showEaAutoSyncDialog by remember { mutableStateOf(false) }
    var showUbisoftAutoSyncDialog by remember { mutableStateOf(false) }
    var showSteamSetupDialog by remember { mutableStateOf(false) }
    var steamApiKeyInput by remember { mutableStateOf("") }
    var steamProfileUrlInput by remember { mutableStateOf("") }
    var steamKeyTestResult by remember { mutableStateOf<SteamKeyTestResult?>(null) }
    var isTestingSteamKey by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<GameInfo?>(null) }
    val scope = rememberCoroutineScope()

    val displayedGames = remember(games, sortOption, searchQuery) {
        val filtered = if (searchQuery.isBlank()) games
        else games.filter { it.title.contains(searchQuery, ignoreCase = true) }
        val sorted = filtered.toMutableList()
        when (sortOption) {
            SortOption.ALPHA_ASC -> sorted.sortBy { it.title.lowercase() }
            SortOption.ALPHA_DESC -> sorted.sortByDescending { it.title.lowercase() }
            SortOption.RELEASE_DESC -> sorted.sortByDescending { it.releaseDate }
            SortOption.RELEASE_ASC -> sorted.sortBy { it.releaseDate }
        }
        sorted
    }

    LaunchedEffect(store) {
        if (store == Store.MANUAL) {
            isLoggedIn = true
            if (games.isEmpty()) {
                try {
                    val cached = library.getGamesForStore(store, forceRefresh = false)
                    if (cached.isNotEmpty()) onGamesUpdated(cached)
                } catch (_: Exception) { }
            }
        } else {
            isLoggedIn = library.isLoggedIn(store)
            if (isLoggedIn && games.isEmpty()) {
                try {
                    val cached = library.getGamesForStore(store, forceRefresh = false)
                    if (cached.isNotEmpty()) onGamesUpdated(cached)
                } catch (_: Exception) { }
            }
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            !isLoggedIn -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Not connected to ${store.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = {
                        if (store == Store.STEAM) {
                            steamApiKeyInput = ""
                            steamProfileUrlInput = ""
                            steamKeyTestResult = null
                            showSteamSetupDialog = true
                        } else {
                            scope.launch {
                                isLoading = true
                                try {
                                    val data = library.getClient(store).getLoginData()
                                    loginUrl = data.url
                                    showLoginDialog = true
                                } catch (e: Exception) {
                                    statusMessage = "Error: ${e.message}"
                                }
                                isLoading = false
                            }
                        }
                    }) {
                        Text("Login with ${store.name}")
                    }
                    val msg = statusMessage
                    if (msg != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(span = { GridItemSpan(3) }) {
                        if (store == Store.MANUAL) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onAddGame,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Add game")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        scope.launch {
                                            isSyncing = true
                                            statusMessage = null
                                            try {
                                                val result = library.getGamesForStore(
                                                    Store.MANUAL,
                                                    forceRefresh = true
                                                )
                                                onGamesUpdated(result)
                                            } catch (e: Exception) {
                                                statusMessage = "Error: ${e.message}"
                                            }
                                            isSyncing = false
                                        }
                                    },
                                    enabled = !isSyncing,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.height(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Fetch metadata")
                                    }
                                }
                            }
                        } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        FilledTonalButton(
                            onClick = {
                                when (store) {
                                    Store.STEAM -> {
                                        statusMessage = null
                                        scope.launch {
                                            isSyncing = true
                                            val hasApiKey = library.steam.hasApiKey()
                                            val hasSteamId = library.steam.getSteamId() != null
                                            if (hasApiKey && hasSteamId) {
                                                try {
                                                    val result = library.getGamesForStore(store, forceRefresh = true)
                                                    onGamesUpdated(result)
                                                } catch (e: Exception) {
                                                    statusMessage = "Sync failed: ${e.message}"
                                                }
                                                isSyncing = false
                                            } else {
                                                isSyncing = false
                                                steamApiKeyInput = library.steam.getApiKey() ?: ""
                                                steamProfileUrlInput = library.steam.getProfileUrl() ?: ""
                                                steamKeyTestResult = null
                                                showSteamSetupDialog = true
                                            }
                                        }
                                    }
                                    Store.ITCH -> {
                                        statusMessage = null
                                        scope.launch {
                                            isSyncing = true
                                            val savedUrl = library.itch.getPurchasesUrl()
                                            if (savedUrl != null) {
                                                itchAutoSyncUrl = savedUrl
                                                showItchAutoSyncDialog = true
                                                isSyncing = false
                                            } else {
                                                try {
                                                    val result = library.getGamesForStore(store, forceRefresh = true)
                                                    onGamesUpdated(result)
                                                } catch (e: Exception) {
                                                    statusMessage = "Sync failed: ${e.message}"
                                                }
                                                isSyncing = false
                                            }
                                        }
                                    }
                                    Store.EA -> {
                                        showEaAutoSyncDialog = true
                                    }
                                    Store.UBISOFT -> {
                                        showUbisoftAutoSyncDialog = true
                                    }
                                    else -> {
                                        statusMessage = null
                                        scope.launch {
                                            isSyncing = true
                                            try {
                                                val result = library.getGamesForStore(store, forceRefresh = true)
                                                onGamesUpdated(result)
                                            } catch (e: Exception) {
                                                statusMessage = "Sync failed: ${e.message}"
                                            }
                                            isSyncing = false
                                        }
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Sync new games")
                            }
                        }
                        FilledTonalButton(
                            onClick = { showLogoutConfirm = true },
                            enabled = !isSyncing
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Remove credentials",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Remove")
                        }
                        }
                        }
                    }

                    val syncMsg = statusMessage
                    if (syncMsg != null) {
                        item(span = { GridItemSpan(3) }) {
                            Text(
                                syncMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    item(span = { GridItemSpan(3) }) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            placeholder = { Text("Search games") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {{
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, "Clear")
                                }
                            }} else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    items(displayedGames, key = { it.appName + it.store.name }) { game ->
                        GameCard(
                            game = game,
                            library = library,
                            onClick = { onGameClick(game) },
                            onEdit = if (store == Store.MANUAL) {{ onEditGame(game) }} else null,
                            onDelete = if (store == Store.MANUAL) {{ showDeleteConfirm = game }} else null
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showSortMenu = !showSortMenu }
                    ) {
                        Icon(Icons.Default.Sort, "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Name A-Z") },
                            onClick = { sortOption = SortOption.ALPHA_ASC; showSortMenu = false },
                            leadingIcon = if (sortOption == SortOption.ALPHA_ASC) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Name Z-A") },
                            onClick = { sortOption = SortOption.ALPHA_DESC; showSortMenu = false },
                            leadingIcon = if (sortOption == SortOption.ALPHA_DESC) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Release newest") },
                            onClick = { sortOption = SortOption.RELEASE_DESC; showSortMenu = false },
                            leadingIcon = if (sortOption == SortOption.RELEASE_DESC) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Release oldest") },
                            onClick = { sortOption = SortOption.RELEASE_ASC; showSortMenu = false },
                            leadingIcon = if (sortOption == SortOption.RELEASE_ASC) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    if (showLoginDialog && loginUrl.isNotEmpty()) {
        LoginWebViewDialog(
            store = store,
            authUrl = loginUrl,
            onDismiss = {
                showLoginDialog = false
                loginUrl = ""
            },
            onCodeReceived = { code ->
                showLoginDialog = false
                loginUrl = ""
                scope.launch {
                    isLoading = true
                    try {
                        val ok = library.getClient(store).completeLogin(code)
                        if (ok) {
                            isLoggedIn = true
                            val result = library.getGamesForStore(store, forceRefresh = true)
                            onGamesUpdated(result)
                        } else {
                            statusMessage = "Login failed"
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error: ${e.message}"
                    }
                    isLoading = false
                }
            }
        )
    }

    if (showItchAutoSyncDialog && itchAutoSyncUrl.isNotEmpty()) {
        ItchAutoSyncWebViewDialog(
            url = itchAutoSyncUrl,
            onDismiss = {
                showItchAutoSyncDialog = false
                itchAutoSyncUrl = ""
            },
            onGamesScraped = { json ->
                showItchAutoSyncDialog = false
                itchAutoSyncUrl = ""
                scope.launch {
                    isSyncing = true
                    try {
                        val ok = library.itch.completeLogin(json)
                        if (ok) {
                            val result = library.getGamesForStore(store, forceRefresh = true)
                            onGamesUpdated(result)
                        } else {
                            statusMessage = "Sync failed — please log in again"
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error: ${e.message}"
                    }
                    isSyncing = false
                }
            }
        )
    }

    if (showEaAutoSyncDialog) {
        LoginWebViewDialog(
            store = Store.EA,
            authUrl = "https://myaccount.ea.com/am/data/1/order-history?dateRange=ALL",
            onDismiss = {
                showEaAutoSyncDialog = false
            },
            onCodeReceived = { json ->
                showEaAutoSyncDialog = false
                scope.launch {
                    isSyncing = true
                    try {
                        val ok = library.ea.completeLogin(json)
                        if (ok) {
                            val result = library.getGamesForStore(store, forceRefresh = true)
                            onGamesUpdated(result)
                        } else {
                            statusMessage = "Sync failed — please log in again"
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error: ${e.message}"
                    }
                    isSyncing = false
                }
            }
        )
    }

    if (showUbisoftAutoSyncDialog) {
        LoginWebViewDialog(
            store = Store.UBISOFT,
            authUrl = "https://www.ubisoft.com/en-gb/account/games-activity",
            onDismiss = {
                showUbisoftAutoSyncDialog = false
            },
            onCodeReceived = { json ->
                showUbisoftAutoSyncDialog = false
                scope.launch {
                    isSyncing = true
                    try {
                        val ok = library.ubisoft.completeLogin(json)
                        if (ok) {
                            val result = library.getGamesForStore(store, forceRefresh = true)
                            onGamesUpdated(result)
                        } else {
                            statusMessage = "Sync failed — please log in again"
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error: ${e.message}"
                    }
                    isSyncing = false
                }
            }
        )
    }

    if (showSteamSetupDialog) {
        AlertDialog(
            onDismissRequest = { showSteamSetupDialog = false },
            title = { Text("Connect Steam") },
            text = {
                Column {
                    Text(
                        "Enter your Steam Web API key and your profile name or URL to sync your library.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Get your API key at: https://steamcommunity.com/dev/apikey",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = steamApiKeyInput,
                        onValueChange = { steamApiKeyInput = it },
                        label = { Text("API Key") },
                        placeholder = { Text("Your Steam Web API key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isTestingSteamKey = true
                                    steamKeyTestResult = null
                                    steamKeyTestResult = library.steam.testApiKey(steamApiKeyInput)
                                    isTestingSteamKey = false
                                }
                            },
                            enabled = steamApiKeyInput.isNotBlank() && !isTestingSteamKey
                        ) {
                            Text(if (isTestingSteamKey) "Testing…" else "Test API key")
                        }
                    }
                    val testResult = steamKeyTestResult
                    if (testResult != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            testResult.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testResult.ok) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = steamProfileUrlInput,
                        onValueChange = { steamProfileUrlInput = it },
                        label = { Text("Profile name") },
                        placeholder = { Text("yourname, 7656119... or full profile URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val key = steamApiKeyInput.trim()
                        val profileUrl = steamProfileUrlInput.trim()
                        if (key.isNotEmpty() && profileUrl.isNotEmpty()) {
                            scope.launch {
                                showSteamSetupDialog = false
                                isSyncing = true
                                try {
                                    library.steam.setApiKey(key)
                                    val steamId = library.steam.resolveAndSaveSteamId(profileUrl)
                                    if (steamId != null) {
                                        isLoggedIn = true
                                        val result = library.getGamesForStore(store, forceRefresh = true)
                                        onGamesUpdated(result)
                                    } else {
                                        statusMessage = "Could not resolve Steam ID from that URL"
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "Error: ${e.message}"
                                }
                                isSyncing = false
                            }
                        }
                    },
                    enabled = steamApiKeyInput.isNotBlank() && steamProfileUrlInput.isNotBlank()
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSteamSetupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Remove credentials") },
            text = { Text("Sign out of ${store.name} and delete the saved login? The synced library for this store will be removed from the app.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    scope.launch {
                        try {
                            library.logout(store)
                            isLoggedIn = false
                            onGamesUpdated(emptyList())
                        } catch (e: Exception) {
                            statusMessage = "Logout failed: ${e.message}"
                        }
                    }
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Game") },
            text = { Text("Remove \"${showDeleteConfirm!!.title}\" from your manual library?") },
            confirmButton = {
                TextButton(onClick = {
                    val game = showDeleteConfirm!!
                    showDeleteConfirm = null
                    scope.launch {
                        library.deleteManualGame(game.appName)
                        onGamesUpdated(games.filter { it.appName != game.appName })
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GameCard(
    game: GameInfo,
    library: GameLibrary,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isCompleted = library.isCompleted(game.store, game.appName)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (game.artCover != null) {
                    AsyncImage(
                        model = game.artCover,
                        contentDescription = game.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                IconButton(
                    onClick = { library.toggleCompleted(game.store, game.appName) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = if (isCompleted) "Mark incomplete" else "Mark complete",
                        tint = if (isCompleted) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = game.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
            if (onEdit != null || onDelete != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.zachvlat.gamelibrary.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zachvlat.gamelibrary.R
import com.zachvlat.gamelibrary.library.GameLibrary
import com.zachvlat.gamelibrary.library.model.GameInfo
import com.zachvlat.gamelibrary.library.model.NowPlayingInfo
import com.zachvlat.gamelibrary.library.model.Store

private data class StoreEntry(
    val store: Store,
    val label: String,
    val iconRes: Int
)

private val stores = listOf(
    StoreEntry(Store.MANUAL, "Manual", R.drawable.ic_manual),
    StoreEntry(Store.UBISOFT, "Ubisoft", R.drawable.ic_ubisoft),
    StoreEntry(Store.GOG, "GOG", R.drawable.ic_gog),
    StoreEntry(Store.EPIC, "Epic", R.drawable.ic_epic),
    StoreEntry(Store.AMAZON, "Amazon", R.drawable.ic_amazon),
    StoreEntry(Store.STEAM, "Steam", R.drawable.ic_steam),
    StoreEntry(Store.ITCH, "itch.io", R.drawable.ic_itch),
    StoreEntry(Store.EA, "EA", R.drawable.ic_ea)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    games: Map<Store, List<GameInfo>>,
    library: GameLibrary,
    onGameClick: (GameInfo) -> Unit = {}
) {
    val totalGames = games.values.sumOf { it.size }
    val nowPlayingList = library.nowPlaying
    var editingNp by remember { mutableStateOf<NowPlayingInfo?>(null) }
    var editSliderValue by remember { mutableFloatStateOf(0f) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var randomGame by remember { mutableStateOf<GameInfo?>(null) }

    val allGamesList = remember(games) { games.values.flatten() }

    val sortedStores = remember(games) {
        stores.sortedByDescending { games[it.store]?.size ?: 0 }
    }

    val storeListState = rememberLazyListState()

    LaunchedEffect(sortedStores) {
        storeListState.scrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
        Spacer(Modifier.height(16.dp))

        Text(
            "My game library:",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "Total games: $totalGames",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        LazyRow(
            state = storeListState,
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to Color.Black,
                                0.94f to Color.Black,
                                1f to Color.Transparent
                            ),
                            startX = 0f,
                            endX = size.width
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedStores, key = { it.store.name }) { entry ->
                StoreCard(
                    label = entry.label,
                    count = games[entry.store]?.size ?: 0,
                    iconRes = entry.iconRes
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledTonalButton(
                onClick = { randomGame = allGamesList.randomOrNull() }
            ) {
                Text("Pick a random game")
            }

            val picked = randomGame
            if (picked != null) {
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.clickable { onGameClick(picked) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (picked.artCover != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(picked.artCover)
                                .crossfade(true)
                                .build(),
                            contentDescription = picked.title,
                            modifier = Modifier
                                .width(120.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = picked.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (nowPlayingList.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(
                "Now Playing",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            nowPlayingList.forEach { np ->
                val game = games[np.store]?.find { it.appName == np.appName }
                NowPlayingRow(
                    nowPlaying = np,
                    game = game,
                    onClick = {
                        editSliderValue = np.completionPercent / 100f
                        editingNp = np
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

        FloatingActionButton(
            onClick = { showSearchSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search games")
        }
    }

    val target = editingNp
    if (target != null) {
        val percent = (editSliderValue * 100).toInt()
        AlertDialog(
            onDismissRequest = { editingNp = null },
            title = { Text("Update Completion") },
            text = {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "$percent%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = editSliderValue,
                        onValueChange = { editSliderValue = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        library.updateNowPlaying(target.copy(completionPercent = percent))
                        editingNp = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNp = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSearchSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var query by remember { mutableStateOf("") }

        val allGameList = remember(games) {
            games.entries.flatMap { (store, list) ->
                list.map { it }
            }
        }

        val results = remember(query, allGameList) {
            if (query.length < 3) emptyList()
            else allGameList.filter { it.title.contains(query, ignoreCase = true) }
        }

        ModalBottomSheet(
            onDismissRequest = { showSearchSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Search All Games",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type at least 3 characters") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) {{
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, "Clear")
                        }
                    }} else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(8.dp))

                if (query.length >= 3 && results.isEmpty()) {
                    Text(
                        "No games found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(results, key = { "${it.store.name}_${it.appName}" }) { game ->
                        SearchResultRow(
                            game = game,
                            onClick = {
                                showSearchSheet = false
                                onGameClick(game)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
    }
    }
}

@Composable
private fun NowPlayingRow(
    nowPlaying: NowPlayingInfo,
    game: GameInfo?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (game?.artCover != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(game.artCover)
                    .crossfade(true)
                    .build(),
                contentDescription = nowPlaying.appName,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game?.title ?: nowPlaying.appName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { nowPlaying.completionPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
        }
    }
}

@Composable
private fun SearchResultRow(game: GameInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val iconRes = when (game.store) {
            Store.EPIC -> R.drawable.ic_epic
            Store.GOG -> R.drawable.ic_gog
            Store.AMAZON -> R.drawable.ic_amazon
            Store.STEAM -> R.drawable.ic_steam
            Store.ITCH -> R.drawable.ic_itch
            Store.EA -> R.drawable.ic_ea
            Store.MANUAL -> R.drawable.ic_manual
            Store.UBISOFT -> R.drawable.ic_ubisoft
        }
        Icon(
            painter = painterResource(iconRes),
            contentDescription = game.store.name,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StoreCard(
    label: String,
    count: Int,
    iconRes: Int
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .width(148.dp)
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

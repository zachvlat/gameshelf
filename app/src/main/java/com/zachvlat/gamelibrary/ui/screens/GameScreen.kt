package com.zachvlat.gamelibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zachvlat.gamelibrary.library.GameLibrary
import com.zachvlat.gamelibrary.library.model.GameInfo
import com.zachvlat.gamelibrary.library.model.NowPlayingInfo

@Composable
fun GameScreen(
    game: GameInfo,
    library: GameLibrary
) {
    var showNowPlayingDialog by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var displayGame by remember(game) { mutableStateOf(game) }
    val nowPlayingEntry = library.nowPlaying.find { it.store == displayGame.store && it.appName == displayGame.appName }

    LaunchedEffect(game) {
        displayGame = library.enrichGame(game)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = displayGame.artCover?.let {
                    ImageRequest.Builder(LocalContext.current)
                        .data(it)
                        .crossfade(true)
                        .build()
                },
                contentDescription = displayGame.title,
                modifier = Modifier
                    .width(140.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayGame.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                val developer = displayGame.developer
                if (developer != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = developer,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = displayGame.store.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        MetadataRow("Store", displayGame.store.name)
        val metadataDeveloper = displayGame.developer
        if (metadataDeveloper != null) MetadataRow("Developer", metadataDeveloper)
        val releaseDate = displayGame.releaseDate
        if (releaseDate != null) MetadataRow("Release Date", releaseDate)
        val genres = displayGame.genres
        if (genres != null && genres.isNotEmpty()) MetadataRow("Genres", genres.joinToString(", "))
        MetadataRow("Offline Play", if (displayGame.canRunOffline) "Supported" else "Not supported")
        MetadataRow("Linux", if (displayGame.isLinuxNative) "Native" else "Not supported")
        MetadataRow("Mac", if (displayGame.isMacNative) "Native" else "Not supported")
        val storeUrl = displayGame.storeUrl
        if (storeUrl != null) {
            val uriHandler = LocalUriHandler.current
            MetadataRow(
                "Store URL",
                storeUrl,
                onClick = { uriHandler.openUri(storeUrl) }
            )
        }

        val description = displayGame.description
        if (description != null) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text(
            "Now Playing",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        if (nowPlayingEntry != null) {
            Text(
                "Completion: ${nowPlayingEntry.completionPercent}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { library.removeNowPlaying(displayGame.store, displayGame.appName) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove from Now Playing")
            }
        } else {
            FilledTonalButton(
                onClick = {
                    sliderValue = 0f
                    showNowPlayingDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to Now Playing")
            }
        }
    }

    if (showNowPlayingDialog) {
        val percent = (sliderValue * 100).toInt()
        AlertDialog(
            onDismissRequest = { showNowPlayingDialog = false },
            title = { Text("Set Completion") },
            text = {
                Column {
                    Text(
                        "How much of this game have you completed?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "$percent%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        library.updateNowPlaying(
                            NowPlayingInfo(displayGame.store, displayGame.appName, percent)
                        )
                        showNowPlayingDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNowPlayingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = if (onClick != null) {
                Modifier.weight(0.65f).clickable(onClick = onClick)
            } else {
                Modifier.weight(0.65f)
            }
        )
    }
}

package com.zachvlat.gamelibrary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zachvlat.gamelibrary.library.GameLibrary
import com.zachvlat.gamelibrary.library.model.GameInfo
import com.zachvlat.gamelibrary.library.model.Store
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualGameFormScreen(
    library: GameLibrary,
    existingGame: GameInfo? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEditing = existingGame != null

    var title by remember { mutableStateOf(TextFieldValue(existingGame?.title ?: "")) }
    var developer by remember { mutableStateOf(TextFieldValue(existingGame?.developer ?: "")) }
    var description by remember { mutableStateOf(TextFieldValue(existingGame?.description ?: "")) }
    var artCover by remember { mutableStateOf(TextFieldValue(existingGame?.artCover ?: "")) }
    var artSquare by remember { mutableStateOf(TextFieldValue(existingGame?.artSquare ?: "")) }
    var artLogo by remember { mutableStateOf(TextFieldValue(existingGame?.artLogo ?: "")) }
    var artBackground by remember { mutableStateOf(TextFieldValue(existingGame?.artBackground ?: "")) }
    var releaseDate by remember { mutableStateOf(TextFieldValue(existingGame?.releaseDate ?: "")) }
    var genresText by remember { mutableStateOf(TextFieldValue(existingGame?.genres?.joinToString(", ") ?: "")) }
    var storeUrl by remember { mutableStateOf(TextFieldValue(existingGame?.storeUrl ?: "")) }
    var canRunOffline by remember { mutableStateOf(existingGame?.canRunOffline ?: false) }
    var isLinuxNative by remember { mutableStateOf(existingGame?.isLinuxNative ?: false) }
    var isMacNative by remember { mutableStateOf(existingGame?.isMacNative ?: false) }

    var titleError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Game" else "Add Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = { Text("Title *") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title is required") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = artCover,
                onValueChange = { artCover = it },
                label = { Text("Cover URL (optional)") },
                placeholder = { Text("https://...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            val coverUrl = artCover.text.trim()
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "Cover preview",
                    modifier = Modifier
                        .size(120.dp, 180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            OutlinedTextField(
                value = developer,
                onValueChange = { developer = it },
                label = { Text("Developer") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = artSquare,
                onValueChange = { artSquare = it },
                label = { Text("Square Cover URL") },
                placeholder = { Text("https://...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = artLogo,
                onValueChange = { artLogo = it },
                label = { Text("Logo URL") },
                placeholder = { Text("https://...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = artBackground,
                onValueChange = { artBackground = it },
                label = { Text("Background URL") },
                placeholder = { Text("https://...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = releaseDate,
                onValueChange = { releaseDate = it },
                label = { Text("Release Date") },
                placeholder = { Text("e.g. 2024-01-15") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = genresText,
                onValueChange = { genresText = it },
                label = { Text("Genres") },
                placeholder = { Text("Action, RPG, Indie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = storeUrl,
                onValueChange = { storeUrl = it },
                label = { Text("Store URL") },
                placeholder = { Text("https://...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Can run offline", modifier = Modifier.weight(1f))
                Switch(checked = canRunOffline, onCheckedChange = { canRunOffline = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Linux native", modifier = Modifier.weight(1f))
                Switch(checked = isLinuxNative, onCheckedChange = { isLinuxNative = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mac native", modifier = Modifier.weight(1f))
                Switch(checked = isMacNative, onCheckedChange = { isMacNative = it })
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (title.text.isBlank()) {
                        titleError = true
                        return@Button
                    }

                    val genres = genresText.text.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    val appName = existingGame?.appName
                        ?: "manual_${System.currentTimeMillis()}"

                    val game = GameInfo(
                        store = Store.MANUAL,
                        appName = appName,
                        title = title.text.trim(),
                        developer = developer.text.trim().ifBlank { null },
                        description = description.text.trim().ifBlank { null },
                        artCover = artCover.text.trim().ifBlank { null },
                        artSquare = artSquare.text.trim().ifBlank { null },
                        artLogo = artLogo.text.trim().ifBlank { null },
                        artBackground = artBackground.text.trim().ifBlank { null },
                        releaseDate = releaseDate.text.trim().ifBlank { null },
                        genres = genres.ifEmpty { null },
                        canRunOffline = canRunOffline,
                        storeUrl = storeUrl.text.trim().ifBlank { null },
                        isLinuxNative = isLinuxNative,
                        isMacNative = isMacNative
                    )

                    scope.launch {
                        if (isEditing) {
                            library.updateManualGame(game)
                        } else {
                            library.addManualGame(game)
                        }
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEditing) "Save Changes" else "Add Game")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

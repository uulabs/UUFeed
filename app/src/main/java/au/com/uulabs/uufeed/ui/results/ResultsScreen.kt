package au.com.uulabs.uufeed.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.uulabs.uufeed.ui.FeedUiState
import au.com.uulabs.uufeed.ui.FeedViewModel
import au.com.uulabs.uufeed.ui.Phase
import au.com.uulabs.uufeed.ui.components.PlaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    state: FeedUiState,
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val searching = state.phase == Phase.Searching
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open right now", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (searching || state.canSearchAgain) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = viewModel::searchAgain,
                        enabled = !searching,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(if (state.results.isEmpty()) "Look further" else "Five more")
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.summary.isNotBlank()) {
                            Text(state.summary, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            if (state.demoEnabled) {
                                "Demo data. These places are invented, not from Google. Picked for a mix, and places you've already seen stay hidden until you close UU Feed."
                            } else {
                                "Picked for a mix of places, not just the five most famous. Places you've already seen stay hidden until you close UU Feed."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                state.notice?.let { notice ->
                    item { Banner(notice, container = MaterialTheme.colorScheme.tertiaryContainer, content = MaterialTheme.colorScheme.onTertiaryContainer) }
                }
                state.error?.let { error ->
                    item { Banner(error, container = MaterialTheme.colorScheme.errorContainer, content = MaterialTheme.colorScheme.onErrorContainer) }
                }
                if (searching && state.results.isEmpty()) {
                    items(5) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (it == 0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Text(
                                            "Checking who's open…",
                                            modifier = Modifier.padding(top = 8.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (state.results.isEmpty()) {
                    item {
                        Text(
                            "No places to show for this pass.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                } else {
                    items(state.results, key = { it.id }) { place ->
                        PlaceCard(
                            place = place,
                            origin = state.origin,
                            onClick = { onOpen(place.id) },
                        )
                    }
                }
                item {
                    Text(
                        if (state.demoEnabled) "Demo illustrations, not Google photos." else "Places and reviews from Google.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Banner(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            color = content,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

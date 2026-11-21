package com.uulabs.uufeed.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.uulabs.uufeed.demo.DemoMode
import com.uulabs.uufeed.domain.Budget
import com.uulabs.uufeed.domain.formatKm
import com.uulabs.uufeed.ui.LocationMode
import com.uulabs.uufeed.ui.FeedUiState
import com.uulabs.uufeed.ui.FeedViewModel
import com.uulabs.uufeed.ui.Phase
import com.uulabs.uufeed.ui.theme.FeedMark

private val ratingOptions = listOf(
    null to "Any",
    3.0 to "3+",
    3.5 to "3.5+",
    4.0 to "4+",
    4.5 to "4.5+",
)

@Composable
fun HomeScreen(
    state: FeedUiState,
    viewModel: FeedViewModel,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted, preferDevice = granted)
    }

    LaunchedEffect(state.permissionAsked) {
        if (state.permissionAsked) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onPermissionResult(granted = true, preferDevice = true)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FeedMark(Modifier.size(56.dp))
                    Column(Modifier.weight(1f)) {
                        Text("UU Feed", style = MaterialTheme.typography.displaySmall)
                        Text(
                            "Five open places. A little variety.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }

                if (DemoMode.available) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Demo places", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Invented places around Civic, for trying the layout. Not from Google. Release builds leave this off.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                            Switch(
                                checked = state.demoEnabled,
                                onCheckedChange = viewModel::setDemoEnabled,
                            )
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Where", style = MaterialTheme.typography.titleLarge)
                        val locationColors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline,
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = state.locationMode == LocationMode.DEVICE,
                                onClick = { viewModel.setLocationMode(LocationMode.DEVICE) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                colors = locationColors,
                            ) { Text("This device") }
                            SegmentedButton(
                                selected = state.locationMode == LocationMode.MANUAL,
                                onClick = { viewModel.setLocationMode(LocationMode.MANUAL) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                colors = locationColors,
                            ) { Text("A suburb") }
                        }
                        if (state.demoEnabled || state.locationMode == LocationMode.DEVICE) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = when {
                                        state.demoEnabled -> "Demo search from Civic, Canberra. The device location is not used."
                                        state.locating -> "Finding you…"
                                        state.permissionGranted && state.locationLabel.isNotBlank() -> state.locationLabel
                                        state.permissionGranted -> "Using your location"
                                        else -> "Location is off. Search from a suburb, or allow it."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (!state.demoEnabled && !state.permissionGranted && state.permissionAsked) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            ),
                                        )
                                    }) { Text("Allow location") }
                                    TextButton(onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null),
                                        )
                                        context.startActivity(intent)
                                    }) { Text("Settings") }
                                }
                            }
                        }
                        if (!state.demoEnabled && (state.locationMode == LocationMode.MANUAL || !state.permissionGranted)) {
                            OutlinedTextField(
                                value = state.manualQuery,
                                onValueChange = viewModel::setManualQuery,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Suburb or address") },
                                placeholder = { Text("Braddon, Canberra") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { viewModel.find() }),
                            )
                            Text(
                                "If a name matches more than one place, an Australian match is preferred.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("What are you after?", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Within ${formatKm(state.radiusKm.toDouble())}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Slider(
                            value = state.radiusKm,
                            onValueChange = { raw ->
                                val snapped = (kotlin.math.round(raw * 2f) / 2f).coerceIn(0.5f, 25f)
                                viewModel.setRadius(snapped)
                            },
                            valueRange = 0.5f..25f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        )
                        Text("Minimum rating", style = MaterialTheme.typography.titleSmall)
                        val chipColors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ratingOptions.forEach { (value, label) ->
                                FilterChip(
                                    selected = state.minRating == value,
                                    onClick = { viewModel.setMinRating(value) },
                                    label = { Text(label) },
                                    colors = chipColors,
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = state.minRating == value,
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                        Text("Budget", style = MaterialTheme.typography.titleSmall)
                        Text(
                            state.budget.description + if (state.budget == Budget.ANY) {
                                ""
                            } else {
                                ". Places with no price on Google are left out."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Budget.entries.forEach { budget ->
                                FilterChip(
                                    selected = state.budget == budget,
                                    onClick = { viewModel.setBudget(budget) },
                                    label = { Text(budget.chip) },
                                    colors = chipColors,
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = state.budget == budget,
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                        OutlinedTextField(
                            value = state.keyword,
                            onValueChange = viewModel::setKeyword,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Craving (optional)") },
                            placeholder = { Text("Ramen, Thai, bakery") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.find() }),
                        )
                    }
                }

                if (state.error != null && state.phase != Phase.Searching) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = state.error,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Button(
                    onClick = viewModel::find,
                    enabled = state.phase != Phase.Searching,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.phase == Phase.Searching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Find five places")
                    }
                }
                Text(
                    "Only places Google says are open right now. Distances are straight-line kilometres.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

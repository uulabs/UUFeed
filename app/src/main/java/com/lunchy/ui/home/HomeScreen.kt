package com.lunchy.ui.home

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import com.lunchy.domain.Budget
import com.lunchy.domain.formatKm
import com.lunchy.ui.LocationMode
import com.lunchy.ui.LunchUiState
import com.lunchy.ui.LunchViewModel
import com.lunchy.ui.Phase
import com.lunchy.ui.theme.LunchMark

private val ratingOptions = listOf(
    null to "Any",
    3.0 to "3+",
    3.5 to "3.5+",
    4.0 to "4+",
    4.5 to "4.5+",
)

@Composable
fun HomeScreen(
    state: LunchUiState,
    viewModel: LunchViewModel,
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
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LunchMark(Modifier.size(56.dp))
                    Column {
                        Text("Lunchy", style = MaterialTheme.typography.displaySmall)
                        Text(
                            "Five open places. A little variety.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = state.locationMode == LocationMode.DEVICE,
                                onClick = { viewModel.setLocationMode(LocationMode.DEVICE) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            ) { Text("This device") }
                            SegmentedButton(
                                selected = state.locationMode == LocationMode.MANUAL,
                                onClick = { viewModel.setLocationMode(LocationMode.MANUAL) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            ) { Text("A suburb") }
                        }
                        if (state.locationMode == LocationMode.DEVICE) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = when {
                                        state.locating -> "Finding you…"
                                        state.permissionGranted && state.locationLabel.isNotBlank() -> state.locationLabel
                                        state.permissionGranted -> "Using your location"
                                        else -> "Location is off. Search from a suburb, or allow it."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (!state.permissionGranted && state.permissionAsked) {
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
                        if (state.locationMode == LocationMode.MANUAL || !state.permissionGranted) {
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
                            onValueChange = viewModel::setRadius,
                            valueRange = 0.5f..25f,
                            steps = 48,
                        )
                        Text("Minimum rating", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ratingOptions.forEach { (value, label) ->
                                FilterChip(
                                    selected = state.minRating == value,
                                    onClick = { viewModel.setMinRating(value) },
                                    label = { Text(label) },
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

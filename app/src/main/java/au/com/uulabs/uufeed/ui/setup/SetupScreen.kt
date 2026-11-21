package au.com.uulabs.uufeed.ui.setup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import au.com.uulabs.uufeed.ui.FeedUiState
import au.com.uulabs.uufeed.ui.FeedViewModel

private const val PLACES_NEW = "https://console.cloud.google.com/apis/library/places.googleapis.com"
private const val PLACES_LEGACY = "https://console.cloud.google.com/apis/library/places-backend.googleapis.com"

private val steps = listOf(
    "Create a Google Cloud project and attach a billing account. Places will not answer without billing. Light personal use stays inside the free monthly allowance. Some new billing accounts have to prepay before Google turns an API on. Older projects usually do not.",
    "Enable Places API (New), or, on an older project, Places API (the legacy one). A key often works with only one of those. UU Feed tries New first, then legacy, and tells you which one it is using.",
    "Create an API key under APIs & Services → Credentials.",
    "Optional, and a good idea: restrict the key to Android apps. Use the package name and SHA-1 on this screen. If you also restrict which APIs the key can call, allow the one you enabled.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    state: FeedUiState,
    viewModel: FeedViewModel,
    allowBack: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    var draft by rememberSaveable { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Places API key") },
                navigationIcon = {
                    if (allowBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Set up UU Feed", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "UU Feed asks Google which places are open near you. Your key is stored only on this phone, encrypted, and is sent with Places requests. It is not written into the app's source.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                steps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(step, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedButton(
                    onClick = { open(context, PLACES_NEW) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open Places API (New)")
                }
                OutlinedButton(
                    onClick = { open(context, PLACES_LEGACY) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open legacy Places API")
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("If you restrict the key", style = MaterialTheme.typography.titleSmall)
                        Text("Package", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("au.com.uulabs.uufeed", style = MaterialTheme.typography.bodyLarge)
                        Text("SHA-1 of this install", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                state.signingSha1.ifBlank { "Unavailable" },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (state.signingSha1.isNotBlank()) {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard.setPrimaryClip(ClipData.newPlainText("SHA-1", state.signingSha1))
                                    Toast.makeText(context, "SHA-1 copied", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy SHA-1")
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key") },
                    placeholder = { Text("Paste the key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                )
                Button(
                    onClick = { viewModel.testPlacesKey(draft) },
                    enabled = !state.keyTesting && draft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.keyTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Test key")
                    }
                }
                state.keyTestMessage?.let { message ->
                    Surface(
                        color = if (state.keyTestOk) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            message,
                            modifier = Modifier.padding(14.dp),
                            color = if (state.keyTestOk) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Button(
                    onClick = {
                        viewModel.savePlacesKey(draft)
                        onSaved()
                    },
                    enabled = draft.isNotBlank() && !state.keyTesting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.keyTestOk) "Save and continue" else "Save without testing")
                }
                Text(
                    "Test key tries Places API (New) with a call that asks only for place IDs. If that product is not enabled, it tries one small legacy Nearby Search. A successful test says which API it is using. Everyday searches cost more once you are past the free monthly allowance. Details are in the README.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (allowBack) {
                    TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

private fun open(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

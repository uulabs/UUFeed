package au.com.uulabs.uufeed.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage

@Composable
fun FeedImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val localId = remember(model) { context.demoDrawableId(model) }
    if (localId != 0) {
        Image(
            painter = painterResource(localId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

private fun Context.demoDrawableId(model: String?): Int {
    if (model == null || !model.startsWith("android.resource://")) return 0
    val name = model.substringAfterLast('/').substringBefore('?')
    if (name.isBlank()) return 0
    return resources.getIdentifier(name, "drawable", packageName)
}

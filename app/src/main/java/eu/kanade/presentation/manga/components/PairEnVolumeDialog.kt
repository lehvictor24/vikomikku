package eu.kanade.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Dialog to pick an EN manga to pair with the current JP manga.
 *
 * @param currentPairedId  The currently paired EN manga id (if any), so we can show "Unpair".
 * @param onPair           Called with the selected manga's id, or null to unpair.
 * @param onDismiss        Called when the dialog should close.
 */
@Composable
fun PairEnVolumeDialog(
    currentPairedId: Long?,
    onPair: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val getFavorites: GetFavorites = remember { Injekt.get() }
    var libraryManga by remember { mutableStateOf<List<Manga>>(emptyList()) }

    LaunchedEffect(Unit) {
        libraryManga = getFavorites.await()
            .sortedBy { it.title.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.pair_en_volume)) },
        text = {
            Column {
                if (currentPairedId != null) {
                    Text(
                        text = stringResource(MR.strings.unpair_en_volume),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPair(null)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                    )
                    HorizontalDivider()
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(libraryManga) { manga ->
                        Text(
                            text = manga.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPair(manga.id)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}

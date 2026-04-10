package eu.kanade.presentation.reader

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.data.dictionary.DictionaryEntry
import eu.kanade.tachiyomi.data.dictionary.Tokenizer
import java.util.Locale

/**
 * Bottom-sheet dictionary popup shown when the user taps a word on a Mokuro OCR overlay.
 *
 * Shows: word, reading, part-of-speech, English definitions, a scrollable context sentence
 * with the tapped word highlighted, and a TTS audio button.
 *
 * @param entry The [DictionaryEntry] to display.
 * @param contextText The full text of the OCR block (used for the context sentence row).
 * @param contextTokens Tokenised version of [contextText] for interactive sentence display.
 * @param onTokenTap Called when the user taps a different word token in the context sentence.
 * @param onDismiss Called when the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DictionaryPopup(
    entry: DictionaryEntry,
    contextText: String,
    contextTokens: List<Tokenizer.Token>,
    onTokenTap: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartialExpansion = false)
    val context = LocalContext.current

    // TTS engine — set up once and disposed when the sheet is gone
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { /* init listener */ }
        engine.language = Locale.JAPANESE
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Header: word + reading + audio ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = entry.word,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "【${entry.reading}】",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.common) {
                        AssistChip(
                            onClick = {},
                            label = { Text("common", fontSize = 10.sp) },
                        )
                    }
                    IconButton(onClick = {
                        tts?.speak(entry.reading, TextToSpeech.QUEUE_FLUSH, null, null)
                    }) {
                        Icon(Icons.Outlined.VolumeUp, contentDescription = "Play audio")
                    }
                }
            }

            // ── Frequency bar ─────────────────────────────────────────────
            if (entry.frequencyRank > 0) {
                LinearProgressIndicator(
                    progress = { (entry.frequencyRank / 5000f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "freq ${entry.frequencyRank}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            // ── Context sentence ──────────────────────────────────────────
            if (contextText.isNotBlank()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    contextTokens.forEach { token ->
                        val isHit = token.text == entry.word || token.text == entry.reading
                        Text(
                            text = token.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isHit -> MaterialTheme.colorScheme.primary
                                token.hasEntry -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isHit) FontWeight.Bold else FontWeight.Normal,
                            modifier = if (token.hasEntry) {
                                Modifier.clickable { onTokenTap(token.text) }
                            } else {
                                Modifier
                            },
                        )
                    }
                }
                HorizontalDivider()
            }

            // ── Meanings ──────────────────────────────────────────────────
            entry.meanings.take(3).forEach { meaning ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(meaning.pos, fontSize = 10.sp)
                        },
                    )
                    meaning.glosses.take(4).forEachIndexed { i, gloss ->
                        Text(
                            text = "${i + 1}. $gloss",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

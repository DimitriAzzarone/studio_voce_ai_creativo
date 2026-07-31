package com.dimitriazzarone.studiovoceai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dimitriazzarone.studiovoceai.audio.AudioPlaybackStatus
import com.dimitriazzarone.studiovoceai.audio.AudioPlayerController
import com.dimitriazzarone.studiovoceai.audio.WaveformExtractor
import com.dimitriazzarone.studiovoceai.audio.WaveformResult
import com.dimitriazzarone.studiovoceai.ui.theme.StudioVoceAITheme
import java.util.Locale
import java.util.concurrent.Future

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudioVoceAITheme {
                StudioVoceHome()
            }
        }
    }
}

private data class AudioMetadata(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?
)

@Composable
private fun StudioVoceHome() {
    val context = LocalContext.current
    val playerController = remember { AudioPlayerController(context.applicationContext) }
    val waveformExtractor = remember { WaveformExtractor(context.applicationContext) }
    var waveformTask by remember { mutableStateOf<Future<*>?>(null) }

    var selectedUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMimeType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSizeBytes by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectionWarning by rememberSaveable { mutableStateOf<String?>(null) }
    var waveformState by remember { mutableStateOf<WaveformUiState>(WaveformUiState.None) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        selectionWarning = null
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            selectionWarning = "Il provider non ha concesso un permesso persistente. Il file resta utilizzabile durante questa sessione, ma potrebbe dover essere selezionato di nuovo in futuro."
        }

        val metadata = readAudioMetadata(context.contentResolver, uri)
        selectedUriString = uri.toString()
        selectedName = metadata.displayName
        selectedMimeType = metadata.mimeType
        selectedSizeBytes = metadata.sizeBytes
    }

    val selectedUri = selectedUriString?.let(Uri::parse)

    LaunchedEffect(selectedUriString) {
        waveformTask?.cancel(true)
        waveformTask = null

        if (selectedUriString == null) {
            playerController.clear()
            waveformState = WaveformUiState.None
        } else {
            val uri = Uri.parse(selectedUriString)
            playerController.prepare(uri)
            waveformState = WaveformUiState.Analyzing
            val expectedUri = selectedUriString
            waveformTask = waveformExtractor.extract(uri) { result ->
                if (selectedUriString == expectedUri) {
                    waveformState = when (result) {
                        is WaveformResult.Success -> WaveformUiState.Ready(result.amplitudes)
                        is WaveformResult.Unavailable -> WaveformUiState.Unavailable(result.message)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            waveformTask?.cancel(true)
            waveformExtractor.shutdown()
            playerController.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            BrandHeader()

            AudioSelectionCard(
                selectedUri = selectedUri,
                selectedAudioName = selectedName,
                selectedMimeType = selectedMimeType,
                selectedSizeBytes = selectedSizeBytes,
                selectionWarning = selectionWarning,
                onSelectAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onRemoveSelection = {
                    selectedUriString = null
                    selectedName = null
                    selectedMimeType = null
                    selectedSizeBytes = null
                    selectionWarning = null
                }
            )

            AudioPlaybackCard(
                hasFile = selectedUri != null,
                controller = playerController,
                waveformState = waveformState
            )

            HonestStatusPanel(
                title = "Partitura",
                message = "Partitura non ancora generata",
                accent = Color(0xFFFFD166)
            )
            HonestStatusPanel(
                title = "Voce e accompagnamento",
                message = "Separazione AI prevista nella fase successiva",
                accent = MaterialTheme.colorScheme.primary
            )
            HonestStatusPanel(
                title = "Testo e karaoke",
                message = "Testo e karaoke non ancora disponibili",
                accent = Color(0xFF9B7BFF)
            )

            Text(
                text = "Riproduzione e waveform appartengono alla fase 2. Separazione AI, testo, karaoke e generazione reale della partitura non sono ancora implementati.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private sealed interface WaveformUiState {
    data object None : WaveformUiState
    data object Analyzing : WaveformUiState
    data class Ready(val amplitudes: List<Float>) : WaveformUiState
    data class Unavailable(val message: String) : WaveformUiState
}

@Composable
private fun AudioPlaybackCard(
    hasFile: Boolean,
    controller: AudioPlayerController,
    waveformState: WaveformUiState
) {
    var dragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(controller.positionMs, dragging) {
        if (!dragging) sliderPosition = controller.positionMs.toFloat()
    }

    val duration = controller.durationMs
    val statusText = if (!hasFile) {
        "Nessun file selezionato"
    } else {
        when (controller.status) {
            AudioPlaybackStatus.NO_FILE -> "Nessun file selezionato"
            AudioPlaybackStatus.LOADING -> "Caricamento"
            AudioPlaybackStatus.READY -> "Pronto"
            AudioPlaybackStatus.PLAYING -> "In riproduzione"
            AudioPlaybackStatus.PAUSED -> "In pausa"
            AudioPlaybackStatus.ENDED -> "Terminato"
            AudioPlaybackStatus.ERROR -> "Errore"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Stato elaborazione",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(statusText, color = MaterialTheme.colorScheme.primary)

            controller.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            when (waveformState) {
                WaveformUiState.None -> Text(
                    "Seleziona un file per analizzare la waveform.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WaveformUiState.Analyzing -> Text(
                    "Analisi waveform in corso",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is WaveformUiState.Unavailable -> Text(
                    waveformState.message,
                    color = MaterialTheme.colorScheme.tertiary
                )
                is WaveformUiState.Ready -> Waveform(
                    amplitudes = waveformState.amplitudes,
                    progress = if (duration > 0L) {
                        (controller.positionMs.toFloat() / duration).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                )
            }

            Slider(
                value = sliderPosition.coerceIn(0f, duration.coerceAtLeast(1L).toFloat()),
                onValueChange = {
                    dragging = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    controller.seekTo(sliderPosition.toLong())
                    dragging = false
                },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                enabled = hasFile && duration > 0L
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(if (dragging) sliderPosition.toLong() else controller.positionMs))
                Text(formatTimeOrUnknown(duration))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = controller::play,
                    enabled = hasFile &&
                        controller.status != AudioPlaybackStatus.ERROR &&
                        controller.status != AudioPlaybackStatus.LOADING,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Play")
                }
                Button(
                    onClick = controller::pause,
                    enabled = controller.status == AudioPlaybackStatus.PLAYING,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pausa")
                }
                OutlinedButton(
                    onClick = controller::stop,
                    enabled = hasFile && controller.status != AudioPlaybackStatus.NO_FILE,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop")
                }
            }
        }
    }
}

@Composable
private fun Waveform(amplitudes: List<Float>, progress: Float) {
    val playedColor = MaterialTheme.colorScheme.primary
    val remainingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        if (amplitudes.isEmpty()) return@Canvas
        val spacing = size.width / amplitudes.size
        val centerY = size.height / 2f
        amplitudes.forEachIndexed { index, amplitude ->
            val x = spacing * index + spacing / 2f
            val halfHeight = (amplitude.coerceIn(0.03f, 1f) * size.height * 0.46f)
            drawLine(
                color = if (index.toFloat() / amplitudes.size <= progress) playedColor else remainingColor,
                start = Offset(x, centerY - halfHeight),
                end = Offset(x, centerY + halfHeight),
                strokeWidth = (spacing * 0.55f).coerceAtLeast(1.5f),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun readAudioMetadata(
    contentResolver: android.content.ContentResolver,
    uri: Uri
): AudioMetadata {
    var displayName = uri.lastPathSegment ?: "File audio selezionato"
    var sizeBytes: Long? = null

    try {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) displayName = cursor.getString(nameIndex)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
            }
        }
    } catch (_: SecurityException) {
    }

    val mimeType = try {
        contentResolver.getType(uri) ?: "Tipo audio non disponibile"
    } catch (_: SecurityException) {
        "Tipo audio non disponibile"
    }

    return AudioMetadata(displayName, mimeType, sizeBytes)
}

private fun formatFileSize(sizeBytes: Long?): String {
    if (sizeBytes == null || sizeBytes < 0) return "Dimensione non disponibile"
    if (sizeBytes < 1024) return "$sizeBytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = sizeBytes.toDouble()
    var unitIndex = -1
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private fun formatTimeOrUnknown(milliseconds: Long): String =
    if (milliseconds > 0L) formatTime(milliseconds) else "--:--"

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, Color(0xFF7657FF))
                    ),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Studio Voce AI",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Musica · Spartito · Karaoke",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun AudioSelectionCard(
    selectedUri: Uri?,
    selectedAudioName: String?,
    selectedMimeType: String?,
    selectedSizeBytes: Long?,
    selectionWarning: String?,
    onSelectAudio: () -> Unit,
    onRemoveSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.AudioFile, null, tint = MaterialTheme.colorScheme.primary)
                Text("File audio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            if (selectedUri == null) {
                Text("Nessun file selezionato", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                MetadataLine("Nome", selectedAudioName ?: "Nome non disponibile")
                MetadataLine("Tipo MIME", selectedMimeType ?: "Tipo audio non disponibile")
                MetadataLine("Dimensione", formatFileSize(selectedSizeBytes))
            }
            selectionWarning?.let {
                Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onSelectAudio,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Scegli un file audio", fontWeight = FontWeight.Bold)
            }
            if (selectedUri != null) {
                OutlinedButton(
                    onClick = onRemoveSelection,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Rimuovi selezione")
                }
            }
        }
    }
}

@Composable
private fun MetadataLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HonestStatusPanel(title: String, message: String, accent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.GraphicEq, null, tint = accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

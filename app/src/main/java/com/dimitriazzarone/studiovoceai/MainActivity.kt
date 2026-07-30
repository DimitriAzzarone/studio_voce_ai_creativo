package com.dimitriazzarone.studiovoceai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.weight
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dimitriazzarone.studiovoceai.ui.theme.StudioVoceAITheme
import java.util.Locale

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
    var selectedUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMimeType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSizeBytes by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectionWarning by rememberSaveable { mutableStateOf<String?>(null) }

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

            HonestStatusPanel(
                title = "Stato elaborazione",
                message = "Nessuna elaborazione eseguita",
                accent = Color(0xFF45D78A)
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
                text = "Prima base Android nativa. Non sono presenti separazione AI, trascrizione o generazione professionale dello spartito.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    displayName = cursor.getString(nameIndex)
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }
    } catch (_: SecurityException) {
        // I metadati possono non essere accessibili con alcuni provider.
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
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "File audio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (selectedUri == null) {
                Text(
                    text = "Nessun file selezionato",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MetadataLine("Nome", selectedAudioName ?: "Nome non disponibile")
                MetadataLine("Tipo MIME", selectedMimeType ?: "Tipo audio non disponibile")
                MetadataLine("Dimensione", formatFileSize(selectedSizeBytes))
            }

            if (selectionWarning != null) {
                Text(
                    text = selectionWarning,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onSelectAudio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Scegli un file audio", fontWeight = FontWeight.Bold)
            }

            if (selectedUri != null) {
                OutlinedButton(
                    onClick = onRemoveSelection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HonestStatusPanel(
    title: String,
    message: String,
    accent: Color
) {
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
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = accent
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

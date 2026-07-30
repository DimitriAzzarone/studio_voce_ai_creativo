package com.dimitriazzarone.studiovoceai.music

data class NoteEvent(
    val midiPitch: Int,
    val startTimeSeconds: Double,
    val durationSeconds: Double,
    val confidence: Float
)

data class TempoInfo(
    val bpm: Double,
    val timeSignatureNumerator: Int,
    val timeSignatureDenominator: Int
)

data class ScoreResult(
    val noteEvents: List<NoteEvent>,
    val tempoInfo: TempoInfo?,
    val keySignature: String?,
    val warnings: List<String>
)

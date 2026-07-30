package com.dimitriazzarone.studiovoceai.music

sealed interface MusicXmlExportResult {
    data object NotImplemented : MusicXmlExportResult
    data class Success(val xml: String) : MusicXmlExportResult
    data class Failure(val message: String) : MusicXmlExportResult
}

interface MusicXmlExporter {
    fun export(scoreResult: ScoreResult): MusicXmlExportResult
}

class NotImplementedMusicXmlExporter : MusicXmlExporter {
    override fun export(scoreResult: ScoreResult): MusicXmlExportResult =
        MusicXmlExportResult.NotImplemented
}
